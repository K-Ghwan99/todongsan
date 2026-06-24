package com.todongsan.insightreputation.insight.service;

import com.todongsan.insightreputation.enums.PublicDataSource;
import com.todongsan.insightreputation.enums.PublicDataType;
import com.todongsan.insightreputation.enums.VisitCertMethod;
import com.todongsan.insightreputation.global.client.ActiveMarketInfoResponse;
import com.todongsan.insightreputation.global.client.MarketClient;
import com.todongsan.insightreputation.global.client.MarketInsightSummaryResponse;
import com.todongsan.insightreputation.global.client.MarketPredictionResponse;
import com.todongsan.insightreputation.global.client.MarketPredictionsPageResponse;
import com.todongsan.insightreputation.global.client.MemberInfoResponse;
import com.todongsan.insightreputation.global.client.MemberPointClient;
import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.insight.dto.MarketDashboardResponse;
import com.todongsan.insightreputation.insight.dto.MarketPriceHistoryResponse;
import com.todongsan.insightreputation.publicdata.entity.PublicDataSnapshot;
import com.todongsan.insightreputation.publicdata.repository.PublicDataSnapshotRepository;
import com.todongsan.insightreputation.visitcertification.repository.VisitCertificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketPriceHistoryService {

    private final MarketClient marketClient;
    private final MemberPointClient memberPointClient;
    private final PublicDataSnapshotRepository publicDataSnapshotRepository;
    private final VisitCertificationRepository visitCertificationRepository;

    public MarketPriceHistoryResponse getPriceHistory(long marketId) {
        // 1. 지역 정보 조회 — RESOURCE_NOT_FOUND이면 그대로 전파
        ActiveMarketInfoResponse marketInfo = marketClient.getActiveMarketInfo(marketId);
        String regionSido = marketInfo.getRegionSido();
        String regionSigu = marketInfo.getRegionSigu();

        // 2. 공공 데이터 조회 — 주간(8주) 우선, 없으면 월간(6개월) 폴백
        LocalDate today = LocalDate.now();

        List<PublicDataSnapshot> weekly = publicDataSnapshotRepository.findRecentPriceData(
                PublicDataSource.REB, PublicDataType.WEEKLY_PRICE_INDEX,
                today.minusWeeks(8), today);
        if (regionSido != null) {
            weekly = applyRegionFilter(weekly, regionSido, regionSigu);
        }

        List<PublicDataSnapshot> snapshots;
        String dataType;

        if (!weekly.isEmpty()) {
            snapshots = weekly;
            dataType = "WEEKLY_PRICE_INDEX";
        } else {
            List<PublicDataSnapshot> monthly = publicDataSnapshotRepository.findRecentPriceData(
                    PublicDataSource.REB, PublicDataType.MONTHLY_PRICE_INDEX,
                    today.minusMonths(6), today);
            if (regionSido != null) {
                monthly = applyRegionFilter(monthly, regionSido, regionSigu);
            }
            snapshots = monthly;
            dataType = "MONTHLY_PRICE_INDEX";
        }

        log.info("가격 이력 조회: marketId={}, regionSido={}, dataType={}, dataCount={}",
                marketId, regionSido, dataType, snapshots.size());

        List<MarketPriceHistoryResponse.PricePoint> priceHistory = snapshots.stream()
                .map(s -> MarketPriceHistoryResponse.PricePoint.builder()
                        .referenceDate(s.getReferenceDate())
                        .value(s.getNumericValue())
                        .build())
                .sorted(Comparator.comparing(MarketPriceHistoryResponse.PricePoint::getReferenceDate))
                .collect(Collectors.toList());

        // 3. 예측 분포 — SETTLED 마켓만 가능, 아니면 빈 리스트
        List<MarketPriceHistoryResponse.OptionDistribution> distribution = Collections.emptyList();
        try {
            MarketInsightSummaryResponse summary = marketClient.getSummary(marketId);
            int total = summary.getMarket().getTotalPredictionCount();
            distribution = summary.getOptionStatistics().stream()
                    .map(opt -> {
                        double ratio = total > 0 ? (double) opt.getPredictionCount() / total : 0.0;
                        return MarketPriceHistoryResponse.OptionDistribution.builder()
                                .optionLabel(opt.getOptionLabel())
                                .ratio(ratio)
                                .isResult(Boolean.TRUE.equals(opt.getIsResult()))
                                .build();
                    })
                    .collect(Collectors.toList());
            log.info("예측 분포 조회 성공: marketId={}, optionCount={}", marketId, distribution.size());
        } catch (CustomException e) {
            // SETTLED 아니거나 예측 데이터 없음 — 빈 리스트로 정상 응답
            log.info("예측 분포 조회 불가 (SETTLED 아님 또는 데이터 없음): marketId={}, errorCode={}",
                    marketId, e.getErrorCode());
        }

        return MarketPriceHistoryResponse.builder()
                .regionSido(regionSido)
                .regionSigu(marketInfo.getRegionSigu())
                .dataType(dataType)
                .priceHistory(priceHistory)
                .latestPredictionDistribution(distribution)
                .build();
    }

    public MarketDashboardResponse getDashboard(Long marketId) {
        ActiveMarketInfoResponse marketInfo = marketClient.getActiveMarketInfo(marketId);

        MarketPriceHistoryResponse priceHistory = getPriceHistory(marketId);

        String trendDirection = null;
        Double changeRate = null;
        List<MarketPriceHistoryResponse.PricePoint> rawRecords = priceHistory.getPriceHistory();
        if (rawRecords != null && rawRecords.size() >= 2) {
            double first = rawRecords.get(0).getValue().doubleValue();
            double last = rawRecords.get(rawRecords.size() - 1).getValue().doubleValue();
            if (first != 0) {
                changeRate = Math.round((last - first) / first * 100 * 100) / 100.0;
                trendDirection = changeRate > 0.05 ? "RISING" : changeRate < -0.05 ? "FALLING" : "FLAT";
            }
        }

        List<MarketDashboardResponse.PriceRecord> records = rawRecords == null ? List.of() :
                rawRecords.stream()
                        .map(p -> new MarketDashboardResponse.PriceRecord(p.getReferenceDate(), p.getValue()))
                        .toList();

        List<MarketDashboardResponse.PredictionOption> predictionDistribution = List.of();
        MarketDashboardResponse.PriceVsPredictionOverlay overlay = null;
        try {
            MarketInsightSummaryResponse summary = marketClient.getSummary(marketId);
            if (summary != null) {
                int total = summary.getMarket().getTotalPredictionCount();
                predictionDistribution = summary.getOptionStatistics().stream()
                        .map(o -> {
                            double ratio = total > 0 ? (double) o.getPredictionCount() / total : 0.0;
                            return new MarketDashboardResponse.PredictionOption(
                                    o.getOptionLabel(), ratio, Boolean.TRUE.equals(o.getIsResult()));
                        })
                        .toList();

                if (trendDirection != null) {
                    List<MarketDashboardResponse.PredictionOption> dist = predictionDistribution;
                    String majorityOption = dist.stream()
                            .max(Comparator.comparingDouble(MarketDashboardResponse.PredictionOption::getRatio))
                            .map(MarketDashboardResponse.PredictionOption::getOptionLabel)
                            .orElse(null);
                    boolean correct = dist.stream()
                            .filter(MarketDashboardResponse.PredictionOption::isResult)
                            .anyMatch(o -> o.getOptionLabel().equals(majorityOption));
                    overlay = new MarketDashboardResponse.PriceVsPredictionOverlay(
                            trendDirection, changeRate, correct, majorityOption);
                }
            }
        } catch (Exception ignored) {
            // SETTLED 아닌 경우 정상 — 빈 distribution 유지
        }

        MarketDashboardResponse.ParticipantStats participantStats = null;
        if (!predictionDistribution.isEmpty()) {
            try {
                participantStats = buildParticipantStats(marketId, marketInfo);
            } catch (Exception e) {
                log.warn("participantStats 빌드 실패 (나머지 응답은 반환): marketId={}", marketId, e);
            }
        }

        String sido = marketInfo.getRegionSido();
        String sigu = marketInfo.getRegionSigu();
        long gpsCertCount;
        long commentCertCount;
        if (sigu != null && !sigu.isBlank()) {
            gpsCertCount = visitCertificationRepository.countBySidoAndSiguAndMethod(sido, sigu, VisitCertMethod.GPS);
            commentCertCount = visitCertificationRepository.countBySidoAndSiguAndMethod(sido, sigu, VisitCertMethod.COMMENT);
        } else if (sido != null) {
            gpsCertCount = visitCertificationRepository.countBySidoAndMethod(sido, VisitCertMethod.GPS);
            commentCertCount = visitCertificationRepository.countBySidoAndMethod(sido, VisitCertMethod.COMMENT);
        } else {
            gpsCertCount = 0;
            commentCertCount = 0;
        }

        return MarketDashboardResponse.builder()
                .marketId(marketId)
                .title(marketInfo.getTitle())
                .regionSido(sido)
                .regionSigu(sigu)
                .priceHistory(new MarketDashboardResponse.PriceHistorySection(
                        priceHistory.getDataType(), records, trendDirection, changeRate))
                .predictionDistribution(predictionDistribution)
                .priceVsPredictionOverlay(overlay)
                .participantStats(participantStats)
                .visitCertStats(new MarketDashboardResponse.VisitCertStats(
                        gpsCertCount + commentCertCount, gpsCertCount, commentCertCount))
                .build();
    }

    private MarketDashboardResponse.ParticipantStats buildParticipantStats(
            Long marketId, ActiveMarketInfoResponse marketInfo) {
        MarketPredictionsPageResponse predictionsPage = marketClient.getPredictionsPage(marketId, 0, 500);
        List<Long> memberIds = predictionsPage.getContent().stream()
                .map(MarketPredictionResponse::getMemberId)
                .distinct()
                .toList();

        int totalParticipants = predictionsPage.getTotalElements().intValue();
        long totalPoolAmount = predictionsPage.getContent().stream()
                .mapToLong(p -> p.getPointAmount().longValue())
                .sum();

        List<MemberInfoResponse> memberInfos = memberPointClient.getBatchMemberInfo(memberIds);

        Map<String, Double> genderDistribution = calcRatioDistribution(
                memberInfos.stream().map(MemberInfoResponse::getGender)
                        .filter(Objects::nonNull).toList());
        Map<String, Double> ageGroupDistribution = calcRatioDistribution(
                memberInfos.stream().map(MemberInfoResponse::getAgeGroup)
                        .filter(Objects::nonNull).toList());

        Double residenceMatchRatio = null;
        String targetSido = marketInfo.getRegionSido();
        if (targetSido != null && !targetSido.equals("전국") && !memberInfos.isEmpty()) {
            long matchCount = memberInfos.stream()
                    .filter(m -> targetSido.equals(m.getResidenceSido()))
                    .count();
            residenceMatchRatio = Math.round((double) matchCount / memberInfos.size() * 1000) / 1000.0;
        }

        return new MarketDashboardResponse.ParticipantStats(
                totalParticipants, totalPoolAmount, genderDistribution, ageGroupDistribution, residenceMatchRatio);
    }

    private Map<String, Double> calcRatioDistribution(List<String> values) {
        if (values.isEmpty()) return Map.of();
        Map<String, Long> counts = values.stream()
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()));
        long total = values.size();
        return counts.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey,
                        e -> Math.round((double) e.getValue() / total * 1000) / 1000.0));
    }

    /**
     * Market 지역 정책 기준 필터링
     * - "전국": region_sido = '전국'
     * - 시도(regionSigu=null): region_sido = :regionSido
     * - 시군구: region_sido = :regionSido AND region_fullpath LIKE '%:regionSigu%'
     */
    private List<PublicDataSnapshot> applyRegionFilter(List<PublicDataSnapshot> snapshots,
                                                        String regionSido, String regionSigu) {
        return snapshots.stream()
                .filter(s -> {
                    if ("전국".equals(regionSido)) {
                        return "전국".equals(s.getRegionSido());
                    } else if (regionSigu != null && !regionSigu.isBlank()) {
                        return regionSido.equals(s.getRegionSido())
                                && s.getRegionFullpath() != null
                                && s.getRegionFullpath().contains(regionSigu);
                    } else {
                        return regionSido.equals(s.getRegionSido());
                    }
                })
                .collect(Collectors.toList());
    }
}

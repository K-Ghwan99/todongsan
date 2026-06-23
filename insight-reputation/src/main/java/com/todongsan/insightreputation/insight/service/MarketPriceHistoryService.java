package com.todongsan.insightreputation.insight.service;

import com.todongsan.insightreputation.enums.PublicDataSource;
import com.todongsan.insightreputation.enums.PublicDataType;
import com.todongsan.insightreputation.global.client.ActiveMarketInfoResponse;
import com.todongsan.insightreputation.global.client.MarketClient;
import com.todongsan.insightreputation.global.client.MarketInsightSummaryResponse;
import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.insight.dto.MarketPriceHistoryResponse;
import com.todongsan.insightreputation.publicdata.entity.PublicDataSnapshot;
import com.todongsan.insightreputation.publicdata.repository.PublicDataSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketPriceHistoryService {

    private final MarketClient marketClient;
    private final PublicDataSnapshotRepository publicDataSnapshotRepository;

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

package com.todongsan.insightreputation.insight.service;

import com.todongsan.insightreputation.enums.InsightReportStatus;
import com.todongsan.insightreputation.enums.PublicDataSource;
import com.todongsan.insightreputation.enums.PublicDataType;
import com.todongsan.insightreputation.enums.VisitCertMethod;
import com.todongsan.insightreputation.insight.dto.ActivityTrendResponse;
import com.todongsan.insightreputation.insight.dto.PlatformOverviewResponse;
import com.todongsan.insightreputation.insight.dto.RegionPriceMapResponse;
import com.todongsan.insightreputation.insight.repository.InsightReportRepository;
import com.todongsan.insightreputation.publicdata.repository.PublicDataSnapshotRepository;
import com.todongsan.insightreputation.reputation.repository.MarketPredictionResultRepository;
import com.todongsan.insightreputation.reputation.repository.ReputationRepository;
import com.todongsan.insightreputation.global.client.BattleClient;
import com.todongsan.insightreputation.global.client.MarketClient;
import com.todongsan.insightreputation.visitcertification.repository.VisitCertificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPlatformInsightService {

    private final ReputationRepository reputationRepository;
    private final VisitCertificationRepository visitCertificationRepository;
    private final InsightReportRepository insightReportRepository;
    private final MarketPredictionResultRepository marketPredictionResultRepository;
    private final PublicDataSnapshotRepository publicDataSnapshotRepository;
    private final MarketClient marketClient;
    private final BattleClient battleClient;

    @Transactional(readOnly = true)
    public PlatformOverviewResponse getOverview() {
        long totalMembers = reputationRepository.count();
        Double avgActivityScore = reputationRepository.avgActivityScore();
        Double avgPredictionAccuracy = reputationRepository.avgPredictionAccuracy();

        List<Object[]> bucketData = reputationRepository.countByActivityScoreBucket();
        Map<String, Double> reputationDistribution = buildRatioDistribution(bucketData, totalMembers);

        Double crowdIntelligenceScore = marketPredictionResultRepository.calculateCrowdIntelligenceScore();

        long totalVisitCerts = visitCertificationRepository.count();
        long gpsCerts = visitCertificationRepository.countByMethod(VisitCertMethod.GPS);
        long commentCerts = visitCertificationRepository.countByMethod(VisitCertMethod.COMMENT);
        Map<String, Double> visitCertMethodRatio = buildVisitCertMethodRatio(gpsCerts, commentCerts, totalVisitCerts);

        long totalDone = insightReportRepository.countByStatus(InsightReportStatus.DONE);
        long totalFailed = insightReportRepository.countByStatus(InsightReportStatus.FAILED);
        long totalPending = insightReportRepository.countByStatus(InsightReportStatus.PENDING);
        Double successRate = (totalDone + totalFailed) > 0
                ? Math.round((double) totalDone / (totalDone + totalFailed) * 1000) / 1000.0
                : null;

        Integer activeMarketsCount = marketClient.getActiveMarketsCount();
        Integer activeBattlesCount = battleClient.getActiveBattlesCount();

        return new PlatformOverviewResponse(
                new PlatformOverviewResponse.PlatformStats(
                        totalMembers, avgActivityScore, avgPredictionAccuracy,
                        totalVisitCerts, activeMarketsCount, activeBattlesCount),
                reputationDistribution,
                crowdIntelligenceScore,
                visitCertMethodRatio,
                new PlatformOverviewResponse.AiReportStats(totalDone, totalFailed, totalPending, successRate)
        );
    }

    @Transactional(readOnly = true)
    public RegionPriceMapResponse getRegionPriceMap() {
        PublicDataType dataType = PublicDataType.WEEKLY_PRICE_INDEX;
        Optional<LocalDate> latestDateOpt = publicDataSnapshotRepository
                .findLatestReferenceDate(PublicDataSource.REB, dataType);

        if (latestDateOpt.isEmpty()) {
            dataType = PublicDataType.MONTHLY_PRICE_INDEX;
            latestDateOpt = publicDataSnapshotRepository
                    .findLatestReferenceDate(PublicDataSource.REB, dataType);
        }
        if (latestDateOpt.isEmpty()) {
            return RegionPriceMapResponse.empty();
        }

        LocalDate latest = latestDateOpt.get();
        LocalDate prev = (dataType == PublicDataType.WEEKLY_PRICE_INDEX)
                ? latest.minusWeeks(1) : latest.minusMonths(1);

        Map<String, Double> latestMap = toSidoIndexMap(
                publicDataSnapshotRepository.findIndexGroupByRegionSido(PublicDataSource.REB, dataType, latest));
        Map<String, Double> prevMap = toSidoIndexMap(
                publicDataSnapshotRepository.findIndexGroupByRegionSido(PublicDataSource.REB, dataType, prev));

        Map<String, Long> visitCertCountMap = visitCertificationRepository.countGroupBySido().stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> ((Number) row[1]).longValue()));

        Set<String> allSidos = new HashSet<>(latestMap.keySet());
        allSidos.addAll(prevMap.keySet());

        String finalDataType = dataType.name();
        List<RegionPriceMapResponse.RegionEntry> regions = allSidos.stream()
                .map(sido -> {
                    Double latestIdx = latestMap.get(sido);
                    Double prevIdx = prevMap.get(sido);
                    Double changePct = (latestIdx != null && prevIdx != null && prevIdx != 0)
                            ? Math.round((latestIdx - prevIdx) / prevIdx * 100 * 100) / 100.0
                            : null;
                    long visitCount = visitCertCountMap.getOrDefault(sido, 0L);
                    return new RegionPriceMapResponse.RegionEntry(
                            sido, latestIdx, prevIdx, changePct, calcDirection(changePct), visitCount);
                })
                .sorted(Comparator.comparing(RegionPriceMapResponse.RegionEntry::regionSido))
                .toList();

        return new RegionPriceMapResponse(latest, finalDataType, regions);
    }

    @Transactional(readOnly = true)
    public ActivityTrendResponse getActivityTrend(int weeks) {
        LocalDateTime from = LocalDateTime.now().minusWeeks(weeks);

        Map<Integer, Long> visitCertByWeek = toWeekMap(
                visitCertificationRepository.countNewCertsByWeek(from));
        List<Object[]> reportRows = insightReportRepository.countCompletedByWeek(from);
        Map<Integer, Long> predictionByWeek = toWeekMap(
                marketPredictionResultRepository.countByWeek(from));

        Set<Integer> allWeeks = new HashSet<>(visitCertByWeek.keySet());
        for (Object[] r : reportRows) {
            allWeeks.add(((Number) r[0]).intValue());
        }
        allWeeks.addAll(predictionByWeek.keySet());

        List<ActivityTrendResponse.WeeklyData> trend = allWeeks.stream()
                .sorted()
                .map(weekKey -> {
                    LocalDate weekStart = yearWeekToMonday(weekKey);
                    long done = 0, failed = 0;
                    for (Object[] r : reportRows) {
                        if (((Number) r[0]).intValue() == weekKey) {
                            done = ((Number) r[1]).longValue();
                            failed = ((Number) r[2]).longValue();
                        }
                    }
                    Double successRate = (done + failed) > 0 ? (double) done / (done + failed) : null;
                    return new ActivityTrendResponse.WeeklyData(
                            weekStart,
                            visitCertByWeek.getOrDefault(weekKey, 0L),
                            done,
                            successRate,
                            predictionByWeek.getOrDefault(weekKey, 0L)
                    );
                })
                .toList();

        return new ActivityTrendResponse("LAST_" + weeks + "_WEEKS", trend);
    }

    private Map<String, Double> buildRatioDistribution(List<Object[]> bucketData, long total) {
        if (total == 0) return Map.of();
        return bucketData.stream().collect(Collectors.toMap(
                row -> (String) row[0],
                row -> Math.round((double) ((Number) row[1]).longValue() / total * 1000) / 1000.0
        ));
    }

    private Map<String, Double> buildVisitCertMethodRatio(long gpsCerts, long commentCerts, long total) {
        if (total == 0) return Map.of();
        Map<String, Double> ratio = new LinkedHashMap<>();
        ratio.put("GPS", Math.round((double) gpsCerts / total * 1000) / 1000.0);
        ratio.put("COMMENT", Math.round((double) commentCerts / total * 1000) / 1000.0);
        return ratio;
    }

    private Map<String, Double> toSidoIndexMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                row -> (String) row[0],
                row -> ((Number) row[1]).doubleValue()
        ));
    }

    private Map<Integer, Long> toWeekMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                row -> ((Number) row[0]).intValue(),
                row -> ((Number) row[1]).longValue()
        ));
    }

    private String calcDirection(Double changePct) {
        if (changePct == null) return null;
        if (changePct > 0.05) return "RISING";
        if (changePct < -0.05) return "FALLING";
        return "FLAT";
    }

    private LocalDate yearWeekToMonday(int yearWeek) {
        int year = yearWeek / 100;
        int week = yearWeek % 100;
        return LocalDate.of(year, 1, 4)
                .with(WeekFields.ISO.weekOfYear(), week)
                .with(DayOfWeek.MONDAY);
    }
}

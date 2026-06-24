# 관리자 대시보드 v9 — 구현 명세

> API 명세 원본: `INSIGHT_API_SPEC.md` v9 섹션 3-3 갱신 / 3-4-1 / 3-5 / 3-6 / 3-7  
> 구현 순서: 1 → 2 → 3 → 4 → 5 (뒤로 갈수록 외부 의존성 증가)

---

## 구현 범위

| 번호 | 기능 | 새 파일 | 수정 파일 |
|---|---|---|---|
| 1 | Battle `analysisData` 구조화 저장 | — | `InsightReportService` |
| 2 | 플랫폼 KPI 대시보드 (`/overview`) | `AdminPlatformInsightController`, `AdminPlatformInsightService`, `PlatformOverviewResponse` | `ReputationRepository`, `VisitCertificationRepository`, `InsightReportRepository`, `MarketPredictionResultRepository` |
| 3 | 전국 가격 지도 (`/regions/price-map`) | `RegionPriceMapResponse` | `AdminPlatformInsightController`, `AdminPlatformInsightService`, `PublicDataSnapshotRepository` |
| 4 | 활동 시계열 추이 (`/activity/trend`) | `ActivityTrendResponse` | `AdminPlatformInsightController`, `AdminPlatformInsightService`, `VisitCertificationRepository`, `InsightReportRepository`, `MarketPredictionResultRepository` |
| 5 | Market 통합 대시보드 (`/dashboard`) | `MarketDashboardResponse` | `MarketPublicDataReferenceController`, `MarketPriceHistoryService`, `VisitCertificationRepository` |

---

## 1. Battle `analysisData` 구조화 저장

### 수정 파일
`InsightReportService.java` — 배틀 AI 리포트 생성 완료 시점(`complete()` 직전)에 analysisData JSON 빌드 후 저장.

### 데이터 소스 (이미 수집된 것)
- `List<BattleVote> rawVotes` — `BattleClient.getBattleVotesRaw()` 결과 (`memberId`, `selectedOption`, `votedAt`)
- `Map<Long, MemberInfoResponse> memberInfoMap` — `MemberPointClient.batchMemberInfo()` 결과 (`gender`, `ageGroup`)
- `BattleResponse battleInfo` — `BattleClient.getBattleInfo(battleId)` 결과 (`sido`, `sigu`, `optionA`, `optionB`)

### optionLabel 매핑 규칙
`BattleVote.selectedOption`은 `"A"` 또는 `"B"`. `analysisData`에는 실제 텍스트를 사용한다.
```java
String labelA = battleInfo.getOptionA();
String labelB = battleInfo.getOptionB();
Map<String, String> optionLabelMap = Map.of("A", labelA, "B", labelB);
```

### analysisData 빌드 로직

```java
private Map<String, Object> buildAnalysisData(
        List<BattleVote> rawVotes,
        Map<Long, MemberInfoResponse> memberInfoMap,
        BattleResponse battleInfo,
        List<Long> certifiedMemberIds  // 별도 조회 필요 (아래 설명)
) {
    int totalVotes = rawVotes.size();
    String labelA = battleInfo.getOptionA();
    String labelB = battleInfo.getOptionB();
    Map<String, String> labelMap = Map.of("A", labelA, "B", labelB);

    // 1. optionDistribution
    Map<String, Long> countByOption = rawVotes.stream()
        .collect(Collectors.groupingBy(v -> labelMap.get(v.getSelectedOption()), Collectors.counting()));

    List<Map<String, Object>> optionDistribution = countByOption.entrySet().stream()
        .map(e -> Map.<String, Object>of(
            "optionLabel", e.getKey(),
            "count", e.getValue(),
            "ratio", totalVotes > 0 ? Math.round((double) e.getValue() / totalVotes * 1000) / 1000.0 : 0.0
        )).toList();

    // 2. genderByOption — 성별 정보 있는 투표자만 집계
    Map<String, Map<String, Double>> genderByOption = rawVotes.stream()
        .filter(v -> memberInfoMap.containsKey(v.getMemberId())
                  && memberInfoMap.get(v.getMemberId()).getGender() != null)
        .collect(Collectors.groupingBy(
            v -> labelMap.get(v.getSelectedOption()),
            Collectors.collectingAndThen(
                Collectors.groupingBy(
                    v -> memberInfoMap.get(v.getMemberId()).getGender(),
                    Collectors.counting()
                ),
                genderCount -> {
                    long total = genderCount.values().stream().mapToLong(Long::longValue).sum();
                    return genderCount.entrySet().stream().collect(
                        Collectors.toMap(Map.Entry::getKey,
                            e -> Math.round((double) e.getValue() / total * 1000) / 1000.0)
                    );
                }
            )
        ));

    // 3. ageGroupByOption — 연령 정보 있는 투표자만 집계 (genderByOption과 동일 패턴, ageGroup으로 교체)

    // 4. visitCertifiedVotePattern
    Set<Long> certifiedSet = new HashSet<>(certifiedMemberIds);
    List<BattleVote> certifiedVotes = rawVotes.stream()
        .filter(v -> certifiedSet.contains(v.getMemberId()))
        .toList();

    Map<String, Long> certifiedCountByOption = certifiedVotes.stream()
        .collect(Collectors.groupingBy(v -> labelMap.get(v.getSelectedOption()), Collectors.counting()));

    int certifiedTotal = certifiedVotes.size();
    List<Map<String, Object>> certifiedDistribution = certifiedCountByOption.entrySet().stream()
        .map(e -> Map.<String, Object>of(
            "optionLabel", e.getKey(),
            "count", e.getValue(),
            "ratio", certifiedTotal > 0 ? Math.round((double) e.getValue() / certifiedTotal * 1000) / 1000.0 : 0.0
        )).toList();

    return Map.of(
        "totalVotes", totalVotes,
        "optionDistribution", optionDistribution,
        "genderByOption", genderByOption,
        "ageGroupByOption", /* 3번과 동일 패턴 */ Map.of(),
        "visitCertifiedVotePattern", Map.of(
            "certifiedVoterCount", certifiedTotal,
            "certifiedOptionDistribution", certifiedDistribution
        )
    );
}
```

### 새로운 Repository 메서드 — `VisitCertificationRepository`

```java
// 특정 시도+시구 방문 인증 회원 ID 목록 조회
@Query("SELECT vc.memberId FROM VisitCertification vc WHERE vc.sido = :sido AND vc.sigu = :sigu")
List<Long> findMemberIdsBySidoAndSigu(@Param("sido") String sido, @Param("sigu") String sigu);

// 시도만으로 조회 (sigu가 null인 경우 대비)
@Query("SELECT vc.memberId FROM VisitCertification vc WHERE vc.sido = :sido")
List<Long> findMemberIdsBySido(@Param("sido") String sido);
```

### InsightReportService 수정 포인트

```java
// generateBattleReportAsync() 내부, Claude 응답 파싱 후 complete() 직전
List<Long> certifiedMemberIds = visitCertificationRepository
    .findMemberIdsBySidoAndSigu(battleInfo.getSido(), battleInfo.getSigu());

Map<String, Object> analysisData = buildAnalysisData(rawVotes, memberInfoMap, battleInfo, certifiedMemberIds);

String analysisDataJson = objectMapper.writeValueAsString(analysisData); // Jackson ObjectMapper 주입 필요
report.complete(claudeResponse, analysisDataJson);  // InsightReport.complete()에 analysisData 파라미터 추가 필요
```

`InsightReport.complete()` 시그니처를 확인하고 `analysisData` 인자를 받도록 수정하거나, complete 전에 별도로 `report.setAnalysisData(analysisDataJson)` 처리.

---

## 2. 플랫폼 KPI 대시보드 (`/admin/insights/overview`)

### 신규 파일

**`insight/controller/AdminPlatformInsightController.java`**

```java
@RestController
@RequestMapping("/api/v1/admin/insights")
@RequiredArgsConstructor
public class AdminPlatformInsightController {

    private final AdminPlatformInsightService adminPlatformInsightService;

    @GetMapping("/overview")
    public ApiResponse<PlatformOverviewResponse> getOverview(
            @RequestHeader(value = "X-Member-Role", required = false) String role) {
        if (!"ADMIN".equals(role)) throw new CustomException(ErrorCode.FORBIDDEN);
        return ApiResponse.success(adminPlatformInsightService.getOverview());
    }

    @GetMapping("/regions/price-map")
    public ApiResponse<RegionPriceMapResponse> getRegionPriceMap(
            @RequestHeader(value = "X-Member-Role", required = false) String role) {
        if (!"ADMIN".equals(role)) throw new CustomException(ErrorCode.FORBIDDEN);
        return ApiResponse.success(adminPlatformInsightService.getRegionPriceMap());
    }

    @GetMapping("/activity/trend")
    public ApiResponse<ActivityTrendResponse> getActivityTrend(
            @RequestHeader(value = "X-Member-Role", required = false) String role,
            @RequestParam(defaultValue = "12") @Min(1) @Max(52) int weeks) {
        if (!"ADMIN".equals(role)) throw new CustomException(ErrorCode.FORBIDDEN);
        return ApiResponse.success(adminPlatformInsightService.getActivityTrend(weeks));
    }
}
```

### 신규 Repository 메서드

**`ReputationRepository`**

```java
// 전체 회원 수 (Reputation 보유)
long countAll(); // JPA 기본 count()로 대체 가능

// activity_score 평균
@Query("SELECT AVG(r.activityScore) FROM Reputation r")
Double avgActivityScore();

// prediction_count > 0인 회원의 prediction_accuracy 평균
@Query("SELECT AVG(r.predictionAccuracy) FROM Reputation r WHERE r.predictionCount > 0")
Double avgPredictionAccuracy();

// activity_score 구간별 집계 (0-20, 21-40, 41-60, 61-80, 81-100)
@Query("""
    SELECT
        CASE
            WHEN r.activityScore BETWEEN 0  AND 20  THEN '0-20'
            WHEN r.activityScore BETWEEN 21 AND 40  THEN '21-40'
            WHEN r.activityScore BETWEEN 41 AND 60  THEN '41-60'
            WHEN r.activityScore BETWEEN 61 AND 80  THEN '61-80'
            ELSE '81-100'
        END as bucket,
        COUNT(r)
    FROM Reputation r
    GROUP BY bucket
    """)
List<Object[]> countByActivityScoreBucket();
```

**`MarketPredictionResultRepository`**

```java
// 플랫폼 전체 예측 정확도 (crowdIntelligenceScore)
@Query("""
    SELECT AVG(CASE WHEN mpr.isCorrect = true THEN 1.0 ELSE 0.0 END) * 100
    FROM MarketPredictionResult mpr
    """)
Double calculateCrowdIntelligenceScore();
```

**`InsightReportRepository`**

```java
// 상태별 건수
long countByStatus(InsightReportStatus status);
```

**`VisitCertificationRepository`** (추가)

```java
long countByMethod(String method); // "GPS" 또는 "COMMENT"
```

### 외부 호출 (선택적, 실패 시 null)

```java
// MarketClient — 활성 마켓 수 (Market Service에 해당 API 있어야 함)
// BattleClient — 활성 배틀 수 (Battle Service에 해당 API 있어야 함)
// 없으면 null 반환, 각 팀에 API 추가 요청
Integer activeMarketsCount = null;
Integer activeBattlesCount = null;
try {
    activeMarketsCount = marketClient.getActiveCount(); // 구현 필요 시 협의
} catch (Exception ignored) { /* null 유지 */ }
```

---

## 3. 전국 가격 지도 (`/admin/insights/regions/price-map`)

### 신규 Repository 메서드 — `PublicDataSnapshotRepository`

```java
// 특정 데이터 타입의 가장 최신 기준일
@Query("""
    SELECT MAX(pds.referenceDate)
    FROM PublicDataSnapshot pds
    WHERE pds.source = 'REB'
      AND pds.dataType = :dataType
      AND pds.itmId = '10001'
    """)
Optional<LocalDate> findLatestReferenceDate(@Param("dataType") String dataType);

// 특정 기준일의 시도별 지수값 조회 (전국 제외)
@Query("""
    SELECT pds.regionSido, AVG(pds.numericValue)
    FROM PublicDataSnapshot pds
    WHERE pds.source = 'REB'
      AND pds.dataType = :dataType
      AND pds.referenceDate = :referenceDate
      AND pds.itmId = '10001'
      AND pds.regionSido != '전국'
      AND pds.regionSido IS NOT NULL
    GROUP BY pds.regionSido
    """)
List<Object[]> findIndexGroupByRegionSido(
    @Param("dataType") String dataType,
    @Param("referenceDate") LocalDate referenceDate
);
```

### 서비스 로직

```java
public RegionPriceMapResponse getRegionPriceMap() {
    // 1. 주간 최신 날짜 조회, 없으면 월간
    String dataType = "WEEKLY_PRICE_INDEX";
    Optional<LocalDate> latestDate = publicDataSnapshotRepository
        .findLatestReferenceDate(dataType);
    if (latestDate.isEmpty()) {
        dataType = "MONTHLY_PRICE_INDEX";
        latestDate = publicDataSnapshotRepository.findLatestReferenceDate(dataType);
    }
    if (latestDate.isEmpty()) return RegionPriceMapResponse.empty();

    LocalDate latest = latestDate.get();
    LocalDate prev = "WEEKLY_PRICE_INDEX".equals(dataType) ? latest.minusWeeks(1) : latest.minusMonths(1);

    // 2. 최신 + 직전 기준일 시도별 지수
    Map<String, Double> latestMap = toSidoIndexMap(
        publicDataSnapshotRepository.findIndexGroupByRegionSido(dataType, latest));
    Map<String, Double> prevMap = toSidoIndexMap(
        publicDataSnapshotRepository.findIndexGroupByRegionSido(dataType, prev));

    // 3. 방문 인증 시도별 카운트
    Map<String, Long> visitCertCountMap = visitCertificationRepository
        .countGroupBySido().stream()
        .collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));

    // 4. 조합
    Set<String> allSidos = new HashSet<>(latestMap.keySet());
    allSidos.addAll(prevMap.keySet());

    List<RegionPriceMapResponse.RegionEntry> regions = allSidos.stream()
        .map(sido -> {
            Double latestIdx = latestMap.get(sido);
            Double prevIdx = prevMap.get(sido);
            Double changePct = (latestIdx != null && prevIdx != null && prevIdx != 0)
                ? Math.round((latestIdx - prevIdx) / prevIdx * 100 * 100) / 100.0
                : null;
            String direction = calcDirection(changePct);
            long visitCount = visitCertCountMap.getOrDefault(sido, 0L);
            return new RegionPriceMapResponse.RegionEntry(sido, latestIdx, prevIdx, changePct, direction, visitCount);
        })
        .sorted(Comparator.comparing(RegionPriceMapResponse.RegionEntry::regionSido))
        .toList();

    return new RegionPriceMapResponse(latest, dataType, regions);
}

private String calcDirection(Double changePct) {
    if (changePct == null) return null;
    if (changePct > 0.05) return "RISING";
    if (changePct < -0.05) return "FALLING";
    return "FLAT";
}
```

**`VisitCertificationRepository`** 추가 메서드:

```java
@Query("SELECT vc.sido, COUNT(vc) FROM VisitCertification vc GROUP BY vc.sido")
List<Object[]> countGroupBySido();
```

---

## 4. 활동 시계열 추이 (`/admin/insights/activity/trend`)

### 신규 Repository 메서드

**`VisitCertificationRepository`**

```java
// 특정 기간 내 주간 최초 방문 인증 건수
@Query("""
    SELECT YEARWEEK(vc.certifiedAt, 1), COUNT(vc)
    FROM VisitCertification vc
    WHERE vc.certifiedAt >= :from
    GROUP BY YEARWEEK(vc.certifiedAt, 1)
    ORDER BY YEARWEEK(vc.certifiedAt, 1)
    """)
List<Object[]> countNewCertsByWeek(@Param("from") LocalDateTime from);
```

**`InsightReportRepository`**

```java
// 주간 AI 리포트 완료/실패 건수 (generated_at 기준)
@Query("""
    SELECT YEARWEEK(ir.generatedAt, 1),
           SUM(CASE WHEN ir.status = 'DONE'   THEN 1 ELSE 0 END),
           SUM(CASE WHEN ir.status = 'FAILED' THEN 1 ELSE 0 END)
    FROM InsightReport ir
    WHERE ir.generatedAt >= :from
    GROUP BY YEARWEEK(ir.generatedAt, 1)
    ORDER BY YEARWEEK(ir.generatedAt, 1)
    """)
List<Object[]> countCompletedByWeek(@Param("from") LocalDateTime from);
```

**`MarketPredictionResultRepository`**

```java
// 주간 예측 결과 처리 건수 (processed_at 기준)
@Query("""
    SELECT YEARWEEK(mpr.processedAt, 1), COUNT(mpr)
    FROM MarketPredictionResult mpr
    WHERE mpr.processedAt >= :from
    GROUP BY YEARWEEK(mpr.processedAt, 1)
    ORDER BY YEARWEEK(mpr.processedAt, 1)
    """)
List<Object[]> countByWeek(@Param("from") LocalDateTime from);
```

### 서비스 로직

```java
public ActivityTrendResponse getActivityTrend(int weeks) {
    LocalDateTime from = LocalDateTime.now().minusWeeks(weeks);

    // 각 Repository 주간 집계 조회 (병렬 처리 권장)
    Map<Integer, Long> visitCertByWeek = toWeekMap(
        visitCertificationRepository.countNewCertsByWeek(from));
    List<Object[]> reportRows = insightReportRepository.countCompletedByWeek(from);
    Map<Integer, Long> predictionByWeek = toWeekMap(
        marketPredictionResultRepository.countByWeek(from));

    // weekKey(YEARWEEK 정수) → LocalDate(월요일) 변환 후 조합
    Set<Integer> allWeeks = new HashSet<>(visitCertByWeek.keySet());
    reportRows.forEach(r -> allWeeks.add(((Number) r[0]).intValue()));
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

private LocalDate yearWeekToMonday(int yearWeek) {
    int year = yearWeek / 100;
    int week = yearWeek % 100;
    return LocalDate.of(year, 1, 4) // ISO week 1의 기준일
        .with(java.time.temporal.WeekFields.ISO.weekOfYear(), week)
        .with(java.time.DayOfWeek.MONDAY);
}
```

---

## 5. Market 통합 대시보드 (`/admin/insights/markets/{marketId}/dashboard`)

### 수정 파일
- `MarketPublicDataReferenceController` — 새 엔드포인트 추가
- `MarketPriceHistoryService` — `getDashboard()` 메서드 추가

### 엔드포인트 추가 (`MarketPublicDataReferenceController`)

```java
@GetMapping("/admin/insights/markets/{marketId}/dashboard")
public ApiResponse<MarketDashboardResponse> getDashboard(
        @PathVariable Long marketId,
        @RequestHeader(value = "X-Member-Role", required = false) String role) {
    if (!"ADMIN".equals(role)) throw new CustomException(ErrorCode.FORBIDDEN);
    return ApiResponse.success(marketPriceHistoryService.getDashboard(marketId));
}
```

### 서비스 로직 (`MarketPriceHistoryService.getDashboard`)

```java
public MarketDashboardResponse getDashboard(Long marketId) {
    // 1. Market 기본 정보 + 지역 (기존 getPriceHistory 로직 재사용)
    ActiveMarketInfoResponse marketInfo = marketClient.getActiveMarketInfo(marketId);
    // RESOURCE_NOT_FOUND 처리는 기존과 동일

    // 2. 가격 이력 (기존 getPriceHistory() 호출)
    MarketPriceHistoryResponse priceHistory = getPriceHistory(marketId);

    // 2-1. trendDirection, changeRate 계산
    String trendDirection = null;
    Double changeRate = null;
    var records = priceHistory.getPriceHistory();
    if (records != null && records.size() >= 2) {
        double first = records.get(0).getValue();
        double last = records.get(records.size() - 1).getValue();
        changeRate = Math.round((last - first) / first * 100 * 100) / 100.0;
        trendDirection = changeRate > 0.05 ? "RISING" : changeRate < -0.05 ? "FALLING" : "FLAT";
    }

    // 3. 예측 분포 (SETTLED만 — 기존 getSummary 호출)
    List<MarketDashboardResponse.PredictionOption> predictionDistribution = List.of();
    MarketDashboardResponse.PriceVsPredictionOverlay overlay = null;
    try {
        MarketInsightSummaryResponse summary = marketClient.getSummary(marketId);
        if (summary != null) {
            predictionDistribution = summary.getOptions().stream()
                .map(o -> new MarketDashboardResponse.PredictionOption(
                    o.getOptionLabel(), o.getRatio(), o.isResult()))
                .toList();

            // overlay 계산
            if (trendDirection != null) {
                String majorityOption = predictionDistribution.stream()
                    .max(Comparator.comparingDouble(MarketDashboardResponse.PredictionOption::ratio))
                    .map(MarketDashboardResponse.PredictionOption::optionLabel)
                    .orElse(null);
                // crowdPredictedCorrectly: isResult=true인 옵션이 majority인지
                boolean correct = predictionDistribution.stream()
                    .filter(MarketDashboardResponse.PredictionOption::isResult)
                    .anyMatch(o -> o.optionLabel().equals(majorityOption));
                overlay = new MarketDashboardResponse.PriceVsPredictionOverlay(
                    trendDirection, changeRate, correct, majorityOption);
            }
        }
    } catch (Exception ignored) { /* SETTLED 아니면 정상 */ }

    // 4. 참여자 인구통계 (SETTLED + 예측 데이터 있을 때)
    MarketDashboardResponse.ParticipantStats participantStats = null;
    if (!predictionDistribution.isEmpty()) {
        try {
            participantStats = buildParticipantStats(marketId, marketInfo);
        } catch (Exception ignored) { /* 실패해도 나머지 데이터는 반환 */ }
    }

    // 5. 방문 인증 현황 (로컬 DB)
    String sido = marketInfo.getRegionSido();
    String sigu = marketInfo.getRegionSigu();
    long gpsCertCount = visitCertificationRepository.countBySidoAndMethod(sido, "GPS");
    long commentCertCount = visitCertificationRepository.countBySidoAndMethod(sido, "COMMENT");
    // sigu가 있으면 더 정밀하게
    if (sigu != null) {
        gpsCertCount = visitCertificationRepository.countBySidoAndSiguAndMethod(sido, sigu, "GPS");
        commentCertCount = visitCertificationRepository.countBySidoAndSiguAndMethod(sido, sigu, "COMMENT");
    }
    var visitCertStats = new MarketDashboardResponse.VisitCertStats(
        gpsCertCount + commentCertCount, gpsCertCount, commentCertCount);

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
        .visitCertStats(visitCertStats)
        .build();
}

private MarketDashboardResponse.ParticipantStats buildParticipantStats(
        Long marketId, ActiveMarketInfoResponse marketInfo) {
    // 예측 참여자 목록 조회 (최대 500명)
    MarketPredictionsPageResponse predictionsPage = marketClient.getPredictions(marketId, 0, 500);
    List<Long> memberIds = predictionsPage.getContent().stream()
        .map(MarketPredictionResponse::getMemberId).toList();

    int totalParticipants = (int) predictionsPage.getTotalElements();
    long totalPoolAmount = predictionsPage.getContent().stream()
        .mapToLong(MarketPredictionResponse::getPointAmount).sum();

    // 회원 인구통계 배치 조회
    List<MemberInfoResponse> memberInfos = memberPointClient.batchMemberInfo(memberIds);
    Map<Long, MemberInfoResponse> infoMap = memberInfos.stream()
        .collect(Collectors.toMap(MemberInfoResponse::getMemberId, m -> m));

    // 성별 분포
    Map<String, Double> genderDistribution = calcRatioDistribution(
        memberInfos.stream().map(MemberInfoResponse::getGender).filter(Objects::nonNull).toList());

    // 연령대 분포
    Map<String, Double> ageGroupDistribution = calcRatioDistribution(
        memberInfos.stream().map(MemberInfoResponse::getAgeGroup).filter(Objects::nonNull).toList());

    // 거주지 일치 비율
    Double residenceMatchRatio = null;
    String targetSido = marketInfo.getRegionSido();
    if (targetSido != null && !targetSido.equals("전국")) {
        long matchCount = memberInfos.stream()
            .filter(m -> targetSido.equals(m.getResidenceSido()))
            .count();
        residenceMatchRatio = memberInfos.isEmpty() ? null
            : Math.round((double) matchCount / memberInfos.size() * 1000) / 1000.0;
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
```

### 추가 Repository 메서드 — `VisitCertificationRepository`

```java
long countBySidoAndMethod(String sido, String method);
long countBySidoAndSiguAndMethod(String sido, String sigu, String method);
long countBySido(String sido);
long countBySidoAndSigu(String sido, String sigu);
```

---

## DTO 구조

### `MarketDashboardResponse`

```java
@Builder
public record MarketDashboardResponse(
    Long marketId,
    String title,
    String regionSido,
    String regionSigu,
    PriceHistorySection priceHistory,
    List<PredictionOption> predictionDistribution,
    PriceVsPredictionOverlay priceVsPredictionOverlay,
    ParticipantStats participantStats,
    VisitCertStats visitCertStats
) {
    public record PriceHistorySection(
        String dataType,
        List<PriceRecord> records,
        String trendDirection,
        Double changeRate
    ) {}
    public record PriceRecord(LocalDate referenceDate, Double value) {}
    public record PredictionOption(String optionLabel, Double ratio, boolean isResult) {}
    public record PriceVsPredictionOverlay(
        String priceTrendDirection, Double priceChangePct,
        Boolean crowdPredictedCorrectly, String majorityOption) {}
    public record ParticipantStats(
        int totalParticipants, long totalPoolAmount,
        Map<String, Double> genderDistribution,
        Map<String, Double> ageGroupDistribution,
        Double residenceMatchRatio) {}
    public record VisitCertStats(long certifiedVisitorCount, long gpsCertCount, long commentCertCount) {}
}
```

### `PlatformOverviewResponse`

```java
public record PlatformOverviewResponse(
    PlatformStats platformStats,
    Map<String, Double> reputationDistribution,
    Double crowdIntelligenceScore,
    Map<String, Double> visitCertMethodRatio,
    AiReportStats aiReportStats
) {
    public record PlatformStats(
        long totalMembersWithReputation,
        Double avgReputationScore,
        Double avgPredictionAccuracy,
        long totalVisitCertifications,
        Integer activeMarketsCount,   // null 가능
        Integer activeBattlesCount    // null 가능
    ) {}
    public record AiReportStats(long totalDone, long totalFailed, long totalPending, Double successRate) {}
}
```

### `RegionPriceMapResponse`

```java
public record RegionPriceMapResponse(
    LocalDate asOf,
    String dataType,
    List<RegionEntry> regions
) {
    public record RegionEntry(
        String regionSido,
        Double latestIndex,
        Double prevIndex,
        Double changePct,
        String direction,
        long visitCertCount
    ) {}

    public static RegionPriceMapResponse empty() {
        return new RegionPriceMapResponse(null, null, List.of());
    }
}
```

### `ActivityTrendResponse`

```java
public record ActivityTrendResponse(String period, List<WeeklyData> weeklyTrend) {
    public record WeeklyData(
        LocalDate weekStart,
        long newVisitCerts,
        long aiReportsCompleted,
        Double aiReportSuccessRate,
        long predictionResultsProcessed
    ) {}
}
```

---

## 주의사항 및 협의 필요 항목

### ⚠️ 외부 서비스 협의 필요

| 항목 | 필요 API | 협의 대상 |
|---|---|---|
| `overview.activeMarketsCount` | Market Service `GET /internal/api/v1/markets/active-count` 또는 유사 API | Market 팀 |
| `overview.activeBattlesCount` | Battle Service `GET /internal/api/v1/battles/active-count` 또는 유사 API | Battle 팀 |

없으면 null 반환 후 API 명세 요청. 블로킹 필요 없음.

### ⚠️ `InsightReport.complete()` 수정

현재 `complete()` 메서드가 `analysisData` 파라미터를 받지 않으면 추가 필요.
`InsightReport` 엔티티에 `analysisData` 필드가 이미 있으므로 별도 세터로 처리해도 됨.

### ⚠️ YEARWEEK 함수

`YEARWEEK(date, 1)` — mode 1은 월요일 시작, ISO 8601 기준. JPA `@Query`에서 네이티브 SQL 함수이므로 `@Query(nativeQuery = true)` 사용 또는 JPQL 대신 네이티브 쿼리로 작성 필요.

### 성능 고려사항

- `overview` : 캐시 적용 권장 (Caffeine, TTL 5분). 집계 쿼리 5개가 동시 실행됨.
- `price-map` : `public_data_snapshot`에 `idx_region_sido` 인덱스 존재 확인 (ERD에 있음).
- `dashboard` participantStats : `MarketPredictions` + `batchMemberInfo` 외부 호출 2회. 응답 지연 예상. 별도 로딩 처리 검토.

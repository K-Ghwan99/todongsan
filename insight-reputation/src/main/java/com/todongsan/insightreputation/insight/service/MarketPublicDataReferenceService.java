package com.todongsan.insightreputation.insight.service;

import com.todongsan.insightreputation.enums.PublicDataSource;
import com.todongsan.insightreputation.enums.PublicDataType;
import com.todongsan.insightreputation.global.client.ActiveMarketInfoResponse;
import com.todongsan.insightreputation.global.client.ClaudeApiClient;
import com.todongsan.insightreputation.global.client.MarketClient;
import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.insight.dto.MarketPublicDataReferenceResponse;
import com.todongsan.insightreputation.publicdata.entity.PublicDataSnapshot;
import com.todongsan.insightreputation.publicdata.repository.PublicDataSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketPublicDataReferenceService {

    private final MarketClient marketClient;
    private final PublicDataSnapshotRepository publicDataSnapshotRepository;
    private final ClaudeApiClient claudeApiClient;

    public MarketPublicDataReferenceResponse getReference(long marketId) {
        // 1. Market 기본 정보 조회 — Market Service 미구현 시 fallback
        ActiveMarketInfoResponse marketInfo = null;
        try {
            marketInfo = marketClient.getActiveMarketInfo(marketId);
        } catch (CustomException e) {
            log.warn("Market 기본 정보 조회 실패, 공공 데이터만으로 fallback 진행: marketId={}, error={}",
                    marketId, e.getMessage());
        } catch (Exception e) {
            log.warn("Market 기본 정보 조회 중 예상치 못한 오류, fallback 진행: marketId={}", marketId, e);
        }

        String marketTitle = marketInfo != null ? marketInfo.getTitle() : "마켓 #" + marketId;
        List<String> optionLabels = marketInfo != null ? marketInfo.getOptionLabels() : Collections.emptyList();
        String regionSido = marketInfo != null ? marketInfo.getRegionSido() : null;
        String regionSigu = marketInfo != null ? marketInfo.getRegionSigu() : null;

        // 2. 공공 데이터 조회 — 주간(8주) 우선, 없으면 월간(3개월) 폴백, 지역 정책 필터 적용
        List<PublicDataSnapshot> publicData = Collections.emptyList();
        LocalDate today = LocalDate.now();
        try {
            if (regionSido != null && !regionSido.isBlank()) {
                publicData = publicDataSnapshotRepository.findRecentPriceData(
                        PublicDataSource.REB, PublicDataType.WEEKLY_PRICE_INDEX,
                        today.minusWeeks(8), today);
                if (publicData.isEmpty()) {
                    publicData = publicDataSnapshotRepository.findRecentPriceData(
                            PublicDataSource.REB, PublicDataType.MONTHLY_PRICE_INDEX,
                            today.minusMonths(3), today);
                }
                publicData = applyRegionFilter(publicData, regionSido, regionSigu);
                log.info("공공 데이터 지역 필터 조회 완료: marketId={}, regionSido={}, regionSigu={}, dataCount={}",
                        marketId, regionSido, regionSigu, publicData.size());
            } else {
                publicData = publicDataSnapshotRepository.findRecentPriceData(
                        PublicDataSource.REB, PublicDataType.WEEKLY_PRICE_INDEX,
                        today.minusWeeks(4), today);
                if (publicData.isEmpty()) {
                    publicData = publicDataSnapshotRepository.findRecentPriceData(
                            PublicDataSource.REB, PublicDataType.MONTHLY_PRICE_INDEX,
                            today.minusMonths(2), today);
                }
                log.info("공공 데이터 전체 조회 완료: marketId={}, dataCount={}", marketId, publicData.size());
            }
        } catch (Exception e) {
            log.warn("공공 데이터 조회 실패: marketId={}", marketId, e);
        }

        // 3. 공공 데이터 없으면 데이터 없음 안내 반환 (Claude 호출 X)
        if (publicData.isEmpty()) {
            log.info("공공 데이터 없음, 안내 응답 반환: marketId={}", marketId);
            return MarketPublicDataReferenceResponse.builder()
                    .title(marketTitle + " — 공공 데이터 참고 자료")
                    .summary("현재 해당 마켓과 관련된 최신 공공 데이터가 없습니다. 공공 데이터는 매주 목요일 및 매월 15일에 업데이트됩니다.")
                    .content("## 안내\n공공 데이터가 아직 수집되지 않았습니다. 잠시 후 다시 시도해 주세요.")
                    .dataAsOf(null)
                    .aiAnalyzed(false)
                    .build();
        }

        LocalDateTime dataAsOf = publicData.stream()
                .map(snap -> snap.getReferenceDate().atStartOfDay())
                .max(LocalDateTime::compareTo)
                .orElse(null);

        // 4. 프롬프트 생성
        String prompt = claudeApiClient.createMarketPublicDataReferencePrompt(
                marketTitle, optionLabels, publicData);

        // 5. Claude extended thinking 호출 — 실패 시 원시 공공 데이터 표로 폴백 (200 유지)
        String rawResult;
        try {
            rawResult = claudeApiClient.analyzeWithThinking(prompt, 2000, 1500);
        } catch (Exception e) {
            log.warn("Claude API 호출 실패, 공공 데이터 원문 표로 폴백: marketId={}, error={}", marketId, e.getMessage());
            return MarketPublicDataReferenceResponse.builder()
                    .title(marketTitle + " — 공공 데이터 참고 자료")
                    .summary("AI 분석을 일시적으로 이용할 수 없습니다. 아래 공공 데이터를 직접 참고해 주세요.")
                    .content(buildRawDataContent(publicData))
                    .dataAsOf(dataAsOf)
                    .aiAnalyzed(false)
                    .build();
        }

        // 6. 응답 파싱 후 DTO 반환 (aiAnalyzed = true)
        return parseAndBuild(rawResult, dataAsOf);
    }

    private MarketPublicDataReferenceResponse parseAndBuild(String rawResult, LocalDateTime dataAsOf) {
        String title = "";
        String summary = "";
        String content = "";

        String[] lines = rawResult.split("\n");
        StringBuilder contentBuilder = new StringBuilder();
        boolean inContent = false;

        for (String line : lines) {
            if (line.startsWith("title:") && !inContent) {
                title = line.substring("title:".length()).trim();
            } else if (line.startsWith("summary:") && !inContent) {
                summary = line.substring("summary:".length()).trim();
            } else if (line.startsWith("content:")) {
                inContent = true;
                String rest = line.substring("content:".length()).trim();
                if (!rest.isEmpty() && !rest.equals("|")) {
                    contentBuilder.append(rest).append("\n");
                }
            } else if (inContent) {
                // YAML 블록 스칼라의 2-space 들여쓰기 제거
                if (line.startsWith("  ")) {
                    contentBuilder.append(line.substring(2)).append("\n");
                } else {
                    contentBuilder.append(line).append("\n");
                }
            }
        }

        content = contentBuilder.toString().trim();

        // 파싱 결과가 비어있으면 원문 전체를 content로 저장
        if (title.isEmpty() && summary.isEmpty() && content.isEmpty()) {
            log.warn("Claude 응답 파싱 실패, 원문을 content로 대체: rawLength={}", rawResult.length());
            content = rawResult;
        }

        return MarketPublicDataReferenceResponse.builder()
                .title(title)
                .summary(summary)
                .content(content)
                .dataAsOf(dataAsOf)
                .aiAnalyzed(true)
                .build();
    }

    private String buildRawDataContent(List<PublicDataSnapshot> publicData) {
        StringBuilder sb = new StringBuilder("## 최근 시장 지표\n\n");
        sb.append("| 기준일 | 지역 | 지표명 | 수치 |\n");
        sb.append("|--------|------|--------|------|\n");
        for (PublicDataSnapshot snap : publicData) {
            String region = snap.getRegionFullpath() != null ? snap.getRegionFullpath()
                    : (snap.getRegionSido() != null ? snap.getRegionSido() : "-");
            String value = snap.getNumericValue() != null
                    ? snap.getNumericValue().stripTrailingZeros().toPlainString() : "-";
            sb.append("| ").append(snap.getReferenceDate())
              .append(" | ").append(region)
              .append(" | ").append(snap.getItmNm() != null ? snap.getItmNm() : "-")
              .append(" | ").append(value).append(" |\n");
        }
        return sb.toString();
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
                .filter(snap -> {
                    if ("전국".equals(regionSido)) {
                        return "전국".equals(snap.getRegionSido());
                    } else if (regionSigu != null && !regionSigu.isBlank()) {
                        return regionSido.equals(snap.getRegionSido())
                                && snap.getRegionFullpath() != null
                                && snap.getRegionFullpath().contains(regionSigu);
                    } else {
                        return regionSido.equals(snap.getRegionSido());
                    }
                })
                .toList();
    }
}

package com.todongsan.insightreputation.global.client;

import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import com.todongsan.insightreputation.publicdata.entity.PublicDataSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeApiClient {

    private final RestTemplate restTemplate;

    @Value("${claude.api.key:}")
    private String apiKey;

    @Value("${claude.api.url:https://api.anthropic.com/v1/messages}")
    private String apiUrl;

    @Value("${claude.api.model:claude-sonnet-4-20250514}")
    private String model;

    public String analyze(String prompt) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Claude API key가 설정되지 않음. 목업 응답 반환");
            return generateMockAnalysis();
        }

        try {
            log.info("Claude API 호출 시작: promptLength={}", prompt.length());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 4000);
            requestBody.put("temperature", 0.1);

            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            requestBody.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-API-Key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            Map<String, Object> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                request,
                Map.class
            ).getBody();

            if (response == null) {
                log.warn("Claude API 응답 없음");
                throw new CustomException(ErrorCode.INSIGHT_REPORT_GENERATION_FAILED);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");

            if (content == null || content.isEmpty()) {
                log.warn("Claude API 응답에 content 없음: response={}", response);
                throw new CustomException(ErrorCode.INSIGHT_REPORT_GENERATION_FAILED);
            }

            String result = (String) content.get(0).get("text");
            if (result == null || result.trim().isEmpty()) {
                log.warn("Claude API 응답 텍스트 비어있음");
                throw new CustomException(ErrorCode.INSIGHT_REPORT_GENERATION_FAILED);
            }

            log.info("Claude API 호출 성공: responseLength={}", result.length());
            return result.trim();

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.error("Claude API 인증 실패");
                throw new CustomException(ErrorCode.INSIGHT_REPORT_GENERATION_FAILED);
            } else if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.error("Claude API 요청 한도 초과");
                throw new CustomException(ErrorCode.INSIGHT_REPORT_GENERATION_FAILED);
            } else {
                log.error("Claude API HTTP 오류: status={}, message={}",
                         e.getStatusCode(), e.getMessage());
                throw new CustomException(ErrorCode.INSIGHT_REPORT_GENERATION_FAILED);
            }
        } catch (ResourceAccessException e) {
            log.error("Claude API 연결 오류: message={}", e.getMessage());
            throw new CustomException(ErrorCode.INSIGHT_REPORT_GENERATION_FAILED);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Claude API 호출 중 예상치 못한 오류", e);
            throw new CustomException(ErrorCode.INSIGHT_REPORT_GENERATION_FAILED);
        }
    }

    private String generateMockAnalysis() {
        return """
                title: Battle 분석 리포트 (Mock)
                summary: 총 1,250명이 참여한 이번 배틀에서 A 옵션이 54.2%로 우세했습니다. 20~30대는 A를 선호한 반면 40대 이상은 B를 더 높이 평가하는 연령별 역전 현상이 나타났습니다.
                content: |
                  ## 배틀 개요
                  총 1,250명이 참여했습니다. A 옵션 678명(54.2%), B 옵션 572명(45.8%)으로 A가 근소하게 앞섰습니다.

                  ## 투표 패턴 분석
                  ### 연령별 분포
                  20대(A 62.3%)·30대(A 58.1%)는 A를 선호했으며, 40대(A 47.3%)·50대 이상(A 38.6%)은 B를 더 높이 평가했습니다.

                  ### 성별 분포
                  남성(A 56.8%)은 A를, 여성(A 48.8%)은 B를 소폭 선호했습니다.

                  ### 지역별 분포
                  서울(A 55.9%), 경기(A 46.6%), 기타(A 58.2%) 순으로 나타났습니다.

                  ## 주요 인사이트
                  1. **연령 역전 현상**: 30대 이하와 40대 이상의 선호가 정반대로 나타났습니다.
                  2. **성별 선호 격차**: 남성은 A, 여성은 B를 소폭 더 선호하는 경향이 있습니다.
                  3. **지역별 편차**: 경기 지역 응답자의 B 선호가 두드러집니다.

                  ## 데이터 신뢰도 및 한계
                  온라인 자발적 참여로 표본 편향 가능성이 있으며, 실거주자와 비거주자를 구분하기 어렵습니다.

                  ## 종합 결론
                  A 옵션이 전체 우세이나 연령·지역별 편차가 크므로, 세부 요인별 세분화 배틀 기획을 권장합니다.
                """;
    }

    /**
     * Battle 분석용 프롬프트 생성
     * - memberInfo를 Java에서 집계하여 Claude에게 완성된 수치 데이터 전달
     * - few-shot 예시 포함으로 출력 형식 및 분량 유도
     */
    public String createBattleAnalysisPrompt(String battleTitle, String optionA, String optionB,
                                           List<BattleVote> votes, List<MemberInfoResponse> memberInfo) {
        long totalVotes = votes.size();
        long optionACount = votes.stream().filter(v -> "A".equals(v.getSelectedOption())).count();
        long optionBCount = votes.stream().filter(v -> "B".equals(v.getSelectedOption())).count();

        StringBuilder prompt = new StringBuilder();

        prompt.append("당신은 부동산 커뮤니티 배틀 데이터 분석 전문가입니다.\n");
        prompt.append("투표 참여자의 인구통계 및 지역 분포 데이터를 분석하여 아래 형식으로 리포트를 작성하세요.\n\n");

        prompt.append("[출력 형식]\n");
        prompt.append("title: {배틀 제목을 포함한 분석 리포트 제목}\n");
        prompt.append("summary: {참여 규모·우세 옵션·핵심 인사이트 2~3문장}\n");
        prompt.append("content: (Markdown, 800자 이상)\n\n");

        prompt.append("## 배틀 개요\n");
        prompt.append("배틀 주제, 참여자 수, 각 옵션 득표를 요약합니다.\n\n");
        prompt.append("## 투표 패턴 분석\n");
        prompt.append("### 연령별 분포\n");
        prompt.append("### 성별 분포\n");
        prompt.append("### 지역별 분포\n\n");
        prompt.append("## 주요 인사이트\n");
        prompt.append("패턴에서 도출된 의미 있는 발견을 3개 이상 서술합니다.\n\n");
        prompt.append("## 데이터 신뢰도 및 한계\n");
        prompt.append("샘플 편향, 응답 왜곡 가능성을 명시합니다.\n\n");
        prompt.append("## 종합 결론\n");
        prompt.append("운영팀을 위한 액션 아이템 포함.\n\n");

        prompt.append("---\n");
        prompt.append("[Few-shot 예시]\n\n");
        prompt.append("입력:\n");
        prompt.append("- 배틀 제목: 강남 vs 마포 — 실거주 만족도 어디가 높을까?\n");
        prompt.append("- 옵션 A: 강남구 (759명, 55.4%)\n");
        prompt.append("- 옵션 B: 마포구 (611명, 44.6%)\n");
        prompt.append("- 총 참여자: 1,370명\n");
        prompt.append("- 연령별 A 선호율: 20대 58.0%, 30대 62.0%, 40대 47.0%, 50대 이상 33.0%\n");
        prompt.append("- 성별 A 선호율: 남성 57.0%, 여성 48.0%\n");
        prompt.append("- 지역별 A 선호율 (상위 5개 지역): 서울특별시 51.0%, 경기도 49.0%, 인천광역시 52.0%, 부산광역시 48.0%, 대구광역시 53.0%\n\n");
        prompt.append("출력:\n");
        prompt.append("title: 강남 vs 마포 실거주 만족도 배틀 AI 분석 리포트\n");
        prompt.append("summary: 1,370명이 참여한 이번 배틀에서 강남구가 55.4%로 소폭 우세했으나 연령·지역별 선호가 뚜렷하게 갈렸습니다. ");
        prompt.append("젊은 층(20~30대)은 강남을 선호한 반면 40대 이상은 마포를 더 높이 평가했으며, 거주 지역과 투표 성향 간 강한 연관성이 관찰되었습니다.\n");
        prompt.append("content: |\n");
        prompt.append("  ## 배틀 개요\n");
        prompt.append("  '강남 vs 마포 실거주 만족도' 배틀에 총 1,370명이 참여했습니다. 강남구(A) 759명(55.4%), 마포구(B) 611명(44.6%)으로 강남구가 근소하게 앞섰습니다.\n\n");
        prompt.append("  ## 투표 패턴 분석\n");
        prompt.append("  ### 연령별 분포\n");
        prompt.append("  20대(A 58%)·30대(A 62%)는 강남구를 선호했으며, 40대(A 47%)·50대 이상(A 33%)은 마포구를 더 높이 평가했습니다. 연령이 높을수록 마포구 선호가 강해지는 역상관이 나타났습니다.\n\n");
        prompt.append("  ### 성별 분포\n");
        prompt.append("  남성(A 57%)은 강남구, 여성(A 48%)은 마포구를 소폭 선호했습니다. 교육·업무 인프라 vs 생활·문화 편의 중심의 선호 차이가 반영된 것으로 보입니다.\n\n");
        prompt.append("  ### 지역별 분포\n");
        prompt.append("  서울 응답자(A 51%)에서 강남구 선호가 근소했으며, 경기도 응답자(A 49%)는 거의 팽팽한 접전을 보였습니다.\n\n");
        prompt.append("  ## 주요 인사이트\n");
        prompt.append("  1. **연령 역전 현상**: 30대 이하와 40대 이상의 선호가 정반대로, 생애주기별 주거 가치관 차이를 보여줍니다.\n");
        prompt.append("  2. **거주 지역 편향**: 현 거주지와 투표 성향이 강하게 연동되어, 지역 정체성이 결과를 크게 좌우했습니다.\n");
        prompt.append("  3. **성별 선호 격차**: 여성의 마포 선호는 홍대·합정 상권 활성화 및 문화 인프라 개선과 연관됩니다.\n\n");
        prompt.append("  ## 데이터 신뢰도 및 한계\n");
        prompt.append("  온라인 자발적 참여로 표본 편향 가능성이 있습니다. 실거주자와 비거주자를 구분하지 못하며, 특정 연령대의 플랫폼 사용률 차이가 결과에 영향을 미쳤을 수 있습니다.\n\n");
        prompt.append("  ## 종합 결론\n");
        prompt.append("  강남·마포 선호는 연령·거주지에 따라 뚜렷하게 갈립니다. 운영팀 제안: 연령대별 세분화 배틀 기획, '교육·교통·상권' 세부 요인별 배틀로 콘텐츠를 확장하면 더 정밀한 수요 분석이 가능합니다.\n");
        prompt.append("---\n\n");
        prompt.append("이제 아래 실제 데이터를 분석하세요.\n\n");

        prompt.append("입력:\n");
        prompt.append("- 배틀 제목: ").append(battleTitle).append("\n");
        prompt.append("- 옵션 A: ").append(optionA)
              .append(" (").append(optionACount).append("명, ")
              .append(String.format("%.1f", optionACount * 100.0 / totalVotes)).append("%)\n");
        prompt.append("- 옵션 B: ").append(optionB)
              .append(" (").append(optionBCount).append("명, ")
              .append(String.format("%.1f", optionBCount * 100.0 / totalVotes)).append("%)\n");
        prompt.append("- 총 참여자: ").append(totalVotes).append("명\n");

        if (!memberInfo.isEmpty()) {
            Map<Long, MemberInfoResponse> memberMap = memberInfo.stream()
                    .collect(Collectors.toMap(MemberInfoResponse::getMemberId, m -> m));

            Map<String, long[]> ageBreakdown = new LinkedHashMap<>();
            ageBreakdown.put("20대", new long[]{0, 0});
            ageBreakdown.put("30대", new long[]{0, 0});
            ageBreakdown.put("40대", new long[]{0, 0});
            ageBreakdown.put("50대 이상", new long[]{0, 0});

            long[] male = {0, 0};
            long[] female = {0, 0};

            Map<String, long[]> regionBreakdown = new LinkedHashMap<>();

            for (BattleVote vote : votes) {
                MemberInfoResponse member = memberMap.get(vote.getMemberId());
                if (member == null) continue;

                int optIdx = "A".equals(vote.getSelectedOption()) ? 0 : 1;

                if (member.getAgeGroup() != null && ageBreakdown.containsKey(member.getAgeGroup())) {
                    ageBreakdown.get(member.getAgeGroup())[optIdx]++;
                }
                if ("MALE".equals(member.getGender())) {
                    male[optIdx]++;
                } else if ("FEMALE".equals(member.getGender())) {
                    female[optIdx]++;
                }
                if (member.getResidenceSido() != null) {
                    regionBreakdown.computeIfAbsent(member.getResidenceSido(), k -> new long[]{0, 0})[optIdx]++;
                }
            }

            prompt.append("- 연령별 A 선호율:\n");
            for (Map.Entry<String, long[]> entry : ageBreakdown.entrySet()) {
                long total = entry.getValue()[0] + entry.getValue()[1];
                if (total > 0) {
                    prompt.append("  ").append(entry.getKey()).append(" ")
                          .append(String.format("%.1f", entry.getValue()[0] * 100.0 / total)).append("%\n");
                }
            }

            prompt.append("- 성별 A 선호율:\n");
            if (male[0] + male[1] > 0) {
                prompt.append("  남성 ").append(String.format("%.1f", male[0] * 100.0 / (male[0] + male[1]))).append("%\n");
            }
            if (female[0] + female[1] > 0) {
                prompt.append("  여성 ").append(String.format("%.1f", female[0] * 100.0 / (female[0] + female[1]))).append("%\n");
            }

            prompt.append("- 지역별 A 선호율 (상위 5개 지역):\n");
            regionBreakdown.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue()[0] + b.getValue()[1], a.getValue()[0] + a.getValue()[1]))
                    .limit(5)
                    .forEach(entry -> {
                        long total = entry.getValue()[0] + entry.getValue()[1];
                        prompt.append("  ").append(entry.getKey()).append(" ")
                              .append(String.format("%.1f", entry.getValue()[0] * 100.0 / total)).append("%\n");
                    });
        }

        return prompt.toString();
    }

    /**
     * Market 분석용 프롬프트 생성
     * - memberInfo를 Java에서 집계하여 Claude에게 완성된 수치 데이터 전달
     * - recentPublicData: 한국부동산원 최근 매매가격지수 (없으면 빈 리스트)
     * - few-shot 예시 포함으로 출력 형식 및 분량 유도
     */
    public String createMarketAnalysisPrompt(String marketTitle,
                                           List<MarketInsightSummaryResponse.OptionStatistics> options,
                                           List<MarketPredictionResponse> predictions,
                                           List<MemberInfoResponse> memberInfo,
                                           List<PublicDataSnapshot> recentPublicData) {
        int totalPredictions = predictions.size();

        StringBuilder prompt = new StringBuilder();

        prompt.append("당신은 부동산 시장 데이터 분석 전문가입니다.\n");
        prompt.append("주어진 마켓 예측 데이터를 바탕으로 아래 형식에 맞춰 상세한 분석 리포트를 작성하세요.\n\n");

        prompt.append("[출력 형식]\n");
        prompt.append("title: {지역·옵션을 포함한 구체적인 제목}\n");
        prompt.append("summary: {핵심 결론 2~3문장, 숫자 포함}\n");
        prompt.append("content: (Markdown, 600자 이상)\n\n");

        prompt.append("## 시장 동향 분석\n");
        prompt.append("마켓 주제와 관련된 현재 시장 상황을 서술합니다.\n\n");
        prompt.append("## 주요 예측 변수\n");
        prompt.append("1. **[변수명]** — 현황과 향후 전망 (수치 포함)\n");
        prompt.append("2. **[변수명]** — ...\n");
        prompt.append("3. **[변수명]** — ...\n\n");
        prompt.append("## 참여자 예측 분포\n");
        prompt.append("| 옵션 | 예측 비율 | 주요 근거 |\n");
        prompt.append("|------|-----------|----------|\n\n");
        prompt.append("## 시나리오별 전망\n");
        prompt.append("- **낙관 시나리오**: ...\n");
        prompt.append("- **기준 시나리오**: ...\n");
        prompt.append("- **비관 시나리오**: ...\n\n");
        prompt.append("## 종합 결론\n");
        prompt.append("데이터 기반 최종 판단과 유의사항.\n\n");

        prompt.append("---\n");
        prompt.append("[Few-shot 예시]\n\n");
        prompt.append("입력:\n");
        prompt.append("- 마켓: 강남구 아파트 2024년 상반기 가격 변동률\n");
        prompt.append("- 옵션 A: 0% 이상 5% 미만 상승 (814명, 65.7%, Pool: 48,920P) ← 정답\n");
        prompt.append("- 옵션 B: 5% 이상 10% 미만 상승 (426명, 34.3%, Pool: 25,560P)\n");
        prompt.append("- 총 예측자: 1,240명\n");
        prompt.append("- 연령별 '0% 이상 5% 미만 상승' 선택율: 20대 61.0%, 30대 68.0%, 40대 64.0%, 50대 이상 71.0%\n");
        prompt.append("- 성별 '0% 이상 5% 미만 상승' 선택율: 남성 63.0%, 여성 69.0%\n\n");
        prompt.append("출력:\n");
        prompt.append("title: 강남구 아파트 2024년 상반기 가격 상승률 AI 분석 리포트\n");
        prompt.append("summary: 1,240명의 예측 데이터 분석 결과, 강남구 아파트 가격은 2024년 상반기 동안 완만한 상승(0~5%) 구간에 머물 가능성이 65.7%로 가장 높습니다. ");
        prompt.append("공급 감소와 전세가율 상승이 하방 지지 요인으로 작용하며, 금리 인하 실현 시 5% 초과 구간으로의 이동도 배제할 수 없습니다.\n");
        prompt.append("content: |\n");
        prompt.append("  ## 시장 동향 분석\n");
        prompt.append("  강남구 아파트 시장은 2024년 상반기 들어 거래량이 회복세로 전환되고 있습니다. ");
        prompt.append("2023년 하반기 급감했던 월별 거래 건수가 3개월 연속 증가하며 관망세였던 실수요자들의 매수 심리가 살아나는 국면입니다.\n\n");
        prompt.append("  ## 주요 예측 변수\n");
        prompt.append("  1. **기준금리** — 한국은행은 현재 동결 기조를 유지 중이나 하반기 0.25%p 인하 가능성이 시장에 40% 내외로 반영되어 있습니다.\n");
        prompt.append("  2. **신규 입주 물량** — 2024년 강남구 입주 예정 물량은 2,100세대로 전년(2,380세대) 대비 11.8% 감소합니다.\n");
        prompt.append("  3. **전세가율** — 현재 75% 수준으로, 실수요 매매 전환이 활발해지는 구간에 진입했습니다.\n\n");
        prompt.append("  ## 참여자 예측 분포\n");
        prompt.append("  | 옵션 | 예측 비율 | 주요 근거 |\n");
        prompt.append("  |------|-----------|----------|\n");
        prompt.append("  | 0~5% 상승 (A) ✓ | **65.7%** | 공급 감소·전세가율 상승에 의한 완만한 우상향 |\n");
        prompt.append("  | 5~10% 상승 (B) | 34.3% | 금리 인하·외지인 수요 유입 시 강한 반등 기대 |\n\n");
        prompt.append("  ## 시나리오별 전망\n");
        prompt.append("  - **낙관 시나리오** (34.3%): 금리 인하 + 토지거래허가구역 해제가 맞물릴 경우 5% 초과 상승 가능.\n");
        prompt.append("  - **기준 시나리오** (65.7%): 금리 동결·물량 감소 속 완만한 상승이 가장 유력.\n");
        prompt.append("  - **비관 시나리오** (5% 미만): 급격한 금리 인상 재개 또는 규제 강화 시 보합 전환 가능.\n\n");
        prompt.append("  ## 종합 결론\n");
        prompt.append("  참여자 다수(65.7%)의 판단과 일치하는 완만한 상승(0~5%)이 기준 시나리오입니다. ");
        prompt.append("금리 정책 변화는 단기 변동성을 유발할 수 있으므로 6개월 단위 재평가가 권장됩니다.\n");
        prompt.append("---\n\n");
        prompt.append("이제 아래 실제 데이터를 분석하세요.\n\n");

        prompt.append("입력:\n");
        prompt.append("- 마켓: ").append(marketTitle).append("\n");

        for (MarketInsightSummaryResponse.OptionStatistics option : options) {
            double pct = totalPredictions > 0
                    ? option.getPredictionCount() * 100.0 / totalPredictions
                    : 0;
            prompt.append("- 옵션: ").append(option.getOptionLabel())
                  .append(" (").append(option.getPredictionCount()).append("명, ")
                  .append(String.format("%.1f", pct)).append("%, Pool: ")
                  .append(option.getPoolAmount()).append("P)");
            if (Boolean.TRUE.equals(option.getIsResult())) {
                prompt.append(" ← 정답");
            }
            prompt.append("\n");
        }
        prompt.append("- 총 예측자: ").append(totalPredictions).append("명\n");

        if (!memberInfo.isEmpty() && !options.isEmpty()) {
            Map<Long, MemberInfoResponse> memberMap = memberInfo.stream()
                    .collect(Collectors.toMap(MemberInfoResponse::getMemberId, m -> m));

            Long firstOptionId = options.get(0).getOptionId();
            String firstLabel = options.get(0).getOptionLabel();

            Map<String, long[]> ageBreakdown = new LinkedHashMap<>();
            ageBreakdown.put("20대", new long[]{0, 0});
            ageBreakdown.put("30대", new long[]{0, 0});
            ageBreakdown.put("40대", new long[]{0, 0});
            ageBreakdown.put("50대 이상", new long[]{0, 0});

            long[] male = {0, 0};
            long[] female = {0, 0};

            for (MarketPredictionResponse pred : predictions) {
                MemberInfoResponse member = memberMap.get(pred.getMemberId());
                if (member == null || pred.getOptionId() == null) continue;

                int optIdx = firstOptionId.equals(pred.getOptionId()) ? 0 : 1;

                if (member.getAgeGroup() != null && ageBreakdown.containsKey(member.getAgeGroup())) {
                    ageBreakdown.get(member.getAgeGroup())[optIdx]++;
                }
                if ("MALE".equals(member.getGender())) {
                    male[optIdx]++;
                } else if ("FEMALE".equals(member.getGender())) {
                    female[optIdx]++;
                }
            }

            prompt.append("- 연령별 '").append(firstLabel).append("' 선택율:\n");
            for (Map.Entry<String, long[]> entry : ageBreakdown.entrySet()) {
                long total = entry.getValue()[0] + entry.getValue()[1];
                if (total > 0) {
                    prompt.append("  ").append(entry.getKey()).append(" ")
                          .append(String.format("%.1f", entry.getValue()[0] * 100.0 / total)).append("%\n");
                }
            }

            prompt.append("- 성별 '").append(firstLabel).append("' 선택율:\n");
            if (male[0] + male[1] > 0) {
                prompt.append("  남성 ").append(String.format("%.1f", male[0] * 100.0 / (male[0] + male[1]))).append("%\n");
            }
            if (female[0] + female[1] > 0) {
                prompt.append("  여성 ").append(String.format("%.1f", female[0] * 100.0 / (female[0] + female[1]))).append("%\n");
            }
        }

        if (recentPublicData != null && !recentPublicData.isEmpty()) {
            appendPublicDataSection(prompt, recentPublicData);
        }

        return prompt.toString();
    }

    private void appendPublicDataSection(StringBuilder prompt, List<PublicDataSnapshot> snapshots) {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 날짜별 → 지역별 지수 정리
        Map<String, Map<String, String>> byDate = new TreeMap<>(Collections.reverseOrder());
        for (PublicDataSnapshot s : snapshots) {
            if (s.getNumericValue() == null) continue;
            String date = s.getReferenceDate().format(dateFmt);
            String region = s.getRegionSido() != null ? s.getRegionSido() : "전국";
            byDate.computeIfAbsent(date, k -> new LinkedHashMap<>())
                  .put(region, s.getNumericValue().toPlainString());
        }

        if (byDate.isEmpty()) return;

        String dataTypeName = snapshots.get(0).getDataType().name().contains("WEEKLY") ? "주간" : "월간";
        prompt.append("- 한국부동산원 아파트 매매가격지수 (").append(dataTypeName).append(", 최근 수집분):\n");

        int dateCount = 0;
        for (Map.Entry<String, Map<String, String>> dateEntry : byDate.entrySet()) {
            if (dateCount++ >= 4) break;
            prompt.append("  [").append(dateEntry.getKey()).append("] ");
            String regionLine = dateEntry.getValue().entrySet().stream()
                    .limit(6)
                    .map(e -> e.getKey() + " " + e.getValue())
                    .collect(Collectors.joining(", "));
            prompt.append(regionLine).append("\n");
        }
    }
}

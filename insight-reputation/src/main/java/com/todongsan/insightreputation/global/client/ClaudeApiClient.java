package com.todongsan.insightreputation.global.client;

import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Claude API를 통한 AI 분석
     * 
     * @param prompt 분석 프롬프트
     * @return 분석 결과
     */
    public String analyze(String prompt) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Claude API key가 설정되지 않음. 목업 응답 반환");
            return generateMockAnalysis();
        }
        
        try {
            log.info("Claude API 호출 시작: promptLength={}", prompt.length());
            
            // Request Body 생성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 1500);
            requestBody.put("temperature", 0.1);
            
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            requestBody.put("messages", messages);
            
            // Headers 설정
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
            
            // 응답에서 content 추출
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
            // CustomException은 그대로 재전파
            throw e;
        } catch (Exception e) {
            log.error("Claude API 호출 중 예상치 못한 오류", e);
            throw new CustomException(ErrorCode.INSIGHT_REPORT_GENERATION_FAILED);
        }
    }

    /**
     * 목업 분석 결과 생성 (개발/테스트용)
     */
    private String generateMockAnalysis() {
        return """
                # Battle 분석 결과 (Mock)
                
                ## 전체 투표 결과
                - 총 투표 수: 1,250명
                - A 옵션: 678명 (54.2%)
                - B 옵션: 572명 (45.8%)
                
                ## 연령대별 분석
                - 20대: A 옵션 선호 (62.3%)
                - 30대: A 옵션 선호 (58.1%)
                - 40대: B 옵션 선호 (52.7%)
                - 50대 이상: B 옵션 선호 (61.4%)
                
                ## 성별 분석
                - 남성: A 옵션 선호 (56.8%)
                - 여성: B 옵션 선호 (51.2%)
                
                ## 지역별 분석
                - 서울: A 옵션 선호 (55.9%)
                - 경기: B 옵션 선호 (53.4%)
                - 기타 지역: A 옵션 선호 (58.2%)
                
                ## 요약
                연령이 낮을수록 A 옵션을 선호하는 경향이 뚜렷하게 나타났습니다. 
                성별로는 남성이 A 옵션을, 여성이 B 옵션을 상대적으로 더 선호했습니다.
                
                *이 분석은 객관적인 데이터 요약이며, 어떤 선택지가 더 좋다는 결론을 내리지 않습니다.*
                """;
    }

    /**
     * Battle 분석용 프롬프트 생성
     */
    public String createBattleAnalysisPrompt(String battleTitle, String optionA, String optionB,
                                           List<BattleVote> votes, List<MemberInfoResponse> memberInfo) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("다음 Battle 투표 결과를 분석해 주세요.\n\n");
        prompt.append("# Battle 정보\n");
        prompt.append("제목: ").append(battleTitle).append("\n");
        prompt.append("옵션 A: ").append(optionA).append("\n");
        prompt.append("옵션 B: ").append(optionB).append("\n\n");
        
        prompt.append("# 투표 데이터 분석\n");
        prompt.append("총 투표 수: ").append(votes.size()).append("명\n\n");
        
        // 기본 집계
        long optionACount = votes.stream().filter(v -> "A".equals(v.getSelectedOption())).count();
        long optionBCount = votes.stream().filter(v -> "B".equals(v.getSelectedOption())).count();
        
        prompt.append("전체 결과:\n");
        prompt.append("- 옵션 A: ").append(optionACount).append("명 (")
              .append(String.format("%.1f", optionACount * 100.0 / votes.size())).append("%)\n");
        prompt.append("- 옵션 B: ").append(optionBCount).append("명 (")
              .append(String.format("%.1f", optionBCount * 100.0 / votes.size())).append("%)\n\n");
        
        // 회원 정보가 있는 경우 세분화 분석 가능
        if (!memberInfo.isEmpty()) {
            prompt.append("# 세부 분석 요청\n");
            prompt.append("위 투표 데이터를 바탕으로 다음과 같이 분석해 주세요:\n");
            prompt.append("1. 연령대별 선호 차이\n");
            prompt.append("2. 성별 선호 차이\n");
            prompt.append("3. 지역별 선호 차이 (가능한 경우)\n\n");
        }
        
        prompt.append("# 분석 지침\n");
        prompt.append("- 투표 결과를 객관적으로 요약하되, 어떤 지역이 더 좋다는 결론을 내리지 마세요.\n");
        prompt.append("- 연령대별, 성별별 선호 차이와 그 특징을 중심으로 요약하세요.\n");
        prompt.append("- 데이터에서 발견되는 흥미로운 패턴이나 특징을 언급해 주세요.\n");
        prompt.append("- 응답은 markdown 형식으로 작성해 주세요.\n");
        prompt.append("- 분석 결과는 1500자 이내로 작성해 주세요.\n");
        
        return prompt.toString();
    }

    /**
     * Market 분석용 프롬프트 생성
     */
    public String createMarketAnalysisPrompt(String marketTitle, List<MarketInsightSummaryResponse.OptionStatistics> options,
                                           List<MarketPredictionResponse> predictions, List<MemberInfoResponse> memberInfo) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("다음 Market 예측 결과를 분석해 주세요.\\n\\n");
        prompt.append("# Market 정보\\n");
        prompt.append("제목: ").append(marketTitle).append("\\n");
        
        // 선택지별 집계 정보
        prompt.append("\\n# 선택지별 집계\\n");
        for (MarketInsightSummaryResponse.OptionStatistics option : options) {
            prompt.append("- ").append(option.getOptionLabel())
                  .append(": ").append(option.getPredictionCount()).append("명")
                  .append(" (Pool: ").append(option.getPoolAmount()).append("P)");
            if (Boolean.TRUE.equals(option.getIsResult())) {
                prompt.append(" ← 정답");
            }
            prompt.append("\\n");
        }
        
        prompt.append("\\n총 참여자: ").append(predictions.size()).append("명\\n\\n");
        
        // 회원 정보가 있는 경우 세분화 분석 가능
        if (!memberInfo.isEmpty()) {
            prompt.append("# 세부 분석 요청\\n");
            prompt.append("위 예측 데이터를 바탕으로 다음과 같이 분석해 주세요:\\n");
            prompt.append("1. 각 선택지를 선택한 사람들의 특성 분석\\n");
            prompt.append("2. 연령대별, 성별 선호 차이\\n");
            prompt.append("3. 지역별 예측 패턴 차이 (가능한 경우)\\n\\n");
        }
        
        prompt.append("# 분석 지침\\n");
        prompt.append("- 특정 선택지(YES/NO)를 추천하거나 예측 방향을 제시하지 마세요.\\n");
        prompt.append("- YES 근거, NO 근거, 관련 통계, 주의사항을 균형 있게 요약하세요.\\n");
        prompt.append("- 예측 결과를 객관적으로 요약하되, 어떤 선택이 더 좋다는 결론을 내리지 마세요.\\n");
        prompt.append("- 연령대별, 성별별 예측 패턴과 그 특징을 중심으로 요약하세요.\\n");
        prompt.append("- 데이터에서 발견되는 흥미로운 패턴이나 특징을 언급해 주세요.\\n");
        prompt.append("- 응답은 markdown 형식으로 작성해 주세요.\\n");
        prompt.append("- 분석 결과는 1500자 이내로 작성해 주세요.\\n");
        
        return prompt.toString();
    }
}
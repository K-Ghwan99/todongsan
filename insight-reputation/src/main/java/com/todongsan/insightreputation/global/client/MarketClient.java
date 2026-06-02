package com.todongsan.insightreputation.global.client;

import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import com.todongsan.insightreputation.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketClient {

    private final RestTemplate restTemplate;
    
    @Value("${client.market.base-url:http://market-service}")
    private String marketServiceBaseUrl;

    /**
     * Market 기본 정보 및 선택지별 집계 조회
     * 
     * @param marketId Market ID
     * @return Market 요약 정보
     */
    public MarketInsightSummaryResponse getSummary(Long marketId) {
        String url = String.format("%s/internal/api/v1/markets/%d/insight-summary", marketServiceBaseUrl, marketId);
        
        try {
            log.info("Market Service 요약 정보 조회 요청: marketId={}", marketId);
            
            ApiResponse<Object> response = restTemplate.getForObject(url, ApiResponse.class);
            
            if (response == null || !response.isSuccess()) {
                log.warn("Market Service 응답 오류: marketId={}, response={}", marketId, response);
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_BAD_RESPONSE);
            }
            
            Object data = response.getData();
            if (data == null) {
                log.warn("Market Service 응답 data 없음: marketId={}", marketId);
                throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            
            // LinkedHashMap을 MarketInsightSummaryResponse로 변환
            if (data instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data;
                
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> marketMap = (java.util.Map<String, Object>) dataMap.get("market");
                
                @SuppressWarnings("unchecked")
                java.util.List<java.util.Map<String, Object>> optionsList = 
                    (java.util.List<java.util.Map<String, Object>>) dataMap.get("optionStatistics");
                
                // Market 정보 매핑
                MarketInsightSummaryResponse.MarketInfo marketInfo = MarketInsightSummaryResponse.MarketInfo.builder()
                    .marketId(((Number) marketMap.get("marketId")).longValue())
                    .title((String) marketMap.get("title"))
                    .category((String) marketMap.get("category"))
                    .status((String) marketMap.get("status"))
                    .closeAt(LocalDateTime.parse((String) marketMap.get("closeAt")))
                    .judgeDate(LocalDateTime.parse((String) marketMap.get("judgeDate")))
                    .resultOptionId(((Number) marketMap.get("resultOptionId")).longValue())
                    .totalPredictionCount(((Number) marketMap.get("totalPredictionCount")).intValue())
                    .totalPoolAmount(new BigDecimal((String) marketMap.get("totalPoolAmount")))
                    .build();
                
                // 옵션 통계 매핑
                List<MarketInsightSummaryResponse.OptionStatistics> optionStatistics = new ArrayList<>();
                for (java.util.Map<String, Object> optionMap : optionsList) {
                    optionStatistics.add(MarketInsightSummaryResponse.OptionStatistics.builder()
                        .optionId(((Number) optionMap.get("optionId")).longValue())
                        .optionLabel((String) optionMap.get("optionLabel"))
                        .predictionCount(((Number) optionMap.get("predictionCount")).intValue())
                        .poolAmount(new BigDecimal((String) optionMap.get("poolAmount")))
                        .isResult((Boolean) optionMap.get("isResult"))
                        .build());
                }
                
                MarketInsightSummaryResponse result = MarketInsightSummaryResponse.builder()
                    .market(marketInfo)
                    .optionStatistics(optionStatistics)
                    .build();
                
                // Market 상태 확인
                if (!"SETTLED".equals(marketInfo.getStatus())) {
                    log.warn("Market이 아직 SETTLED 상태가 아님: marketId={}, status={}", marketId, marketInfo.getStatus());
                    throw new CustomException(ErrorCode.INSIGHT_REPORT_SOURCE_DATA_NOT_READY);
                }
                
                log.info("Market 요약 정보 조회 성공: marketId={}, status={}", marketId, marketInfo.getStatus());
                return result;
            }
            
            log.warn("Market Service 응답 타입 오류: marketId={}, dataType={}", marketId, data.getClass());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_BAD_RESPONSE);
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Market 없음: marketId={}", marketId);
                throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
            } else {
                log.error("Market Service HTTP 오류: marketId={}, status={}, message={}", 
                         marketId, e.getStatusCode(), e.getMessage());
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }
        } catch (ResourceAccessException e) {
            log.error("Market Service 연결 오류: marketId={}, message={}", marketId, e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        } catch (CustomException e) {
            // CustomException은 그대로 재전파
            throw e;
        } catch (Exception e) {
            log.error("Market Service 호출 중 예상치 못한 오류: marketId={}", marketId, e);
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    /**
     * Market 예측 데이터 조회 (페이지네이션)
     * 
     * @param marketId Market ID
     * @return 예측 데이터 목록
     */
    public List<MarketPredictionResponse> getPredictions(Long marketId) {
        List<MarketPredictionResponse> allPredictions = new ArrayList<>();
        int page = 0;
        int size = 500;
        boolean hasNext = true;
        
        while (hasNext) {
            String url = String.format("%s/internal/api/v1/markets/%d/insight-predictions?page=%d&size=%d", 
                                     marketServiceBaseUrl, marketId, page, size);
            
            try {
                log.info("Market Service 예측 데이터 조회 요청: marketId={}, page={}", marketId, page);
                
                ApiResponse<Object> response = restTemplate.getForObject(url, ApiResponse.class);
                
                if (response == null || !response.isSuccess()) {
                    log.warn("Market Service 응답 오류: marketId={}, page={}, response={}", marketId, page, response);
                    throw new CustomException(ErrorCode.EXTERNAL_SERVICE_BAD_RESPONSE);
                }
                
                Object data = response.getData();
                if (data == null) {
                    log.warn("Market Service 응답 data 없음: marketId={}, page={}", marketId, page);
                    break;
                }
                
                // LinkedHashMap을 MarketPredictionsPageResponse로 변환
                if (data instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data;
                    
                    @SuppressWarnings("unchecked")
                    java.util.List<java.util.Map<String, Object>> contentList = 
                        (java.util.List<java.util.Map<String, Object>>) dataMap.get("content");
                    
                    Integer currentPage = ((Number) dataMap.get("page")).intValue();
                    Integer totalPages = ((Number) dataMap.get("totalPages")).intValue();
                    
                    // 예측 데이터 매핑
                    for (java.util.Map<String, Object> predictionMap : contentList) {
                        allPredictions.add(MarketPredictionResponse.builder()
                            .predictionId(((Number) predictionMap.get("predictionId")).longValue())
                            .memberId(((Number) predictionMap.get("memberId")).longValue())
                            .optionId(((Number) predictionMap.get("optionId")).longValue())
                            .optionLabel((String) predictionMap.get("optionLabel"))
                            .pointAmount(new BigDecimal((String) predictionMap.get("pointAmount")))
                            .priceSnapshot(new BigDecimal((String) predictionMap.get("priceSnapshot")))
                            .contractQuantity(new BigDecimal((String) predictionMap.get("contractQuantity")))
                            .status((String) predictionMap.get("status"))
                            .isCorrect((Boolean) predictionMap.get("isCorrect"))
                            .participatedAt(LocalDateTime.parse((String) predictionMap.get("participatedAt")))
                            .build());
                    }
                    
                    hasNext = (currentPage + 1) < totalPages;
                    page++;
                    
                    log.info("Market 예측 데이터 페이지 처리: marketId={}, page={}, contentSize={}, hasNext={}", 
                            marketId, currentPage, contentList.size(), hasNext);
                } else {
                    log.warn("Market Service 응답 타입 오류: marketId={}, page={}, dataType={}", 
                            marketId, page, data.getClass());
                    break;
                }
                
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    log.warn("Market 또는 예측 데이터 없음: marketId={}, page={}", marketId, page);
                    break;
                } else {
                    log.error("Market Service HTTP 오류: marketId={}, page={}, status={}, message={}", 
                             marketId, page, e.getStatusCode(), e.getMessage());
                    throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
                }
            } catch (ResourceAccessException e) {
                log.error("Market Service 연결 오류: marketId={}, page={}, message={}", marketId, page, e.getMessage());
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
            } catch (CustomException e) {
                // CustomException은 그대로 재전파
                throw e;
            } catch (Exception e) {
                log.error("Market Service 호출 중 예상치 못한 오류: marketId={}, page={}", marketId, page, e);
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }
        }
        
        log.info("Market 예측 데이터 수집 완료: marketId={}, totalPredictions={}", marketId, allPredictions.size());
        return allPredictions;
    }
}
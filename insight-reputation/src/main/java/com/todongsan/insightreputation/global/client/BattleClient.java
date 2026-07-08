package com.todongsan.insightreputation.global.client;

import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import com.todongsan.insightreputation.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class BattleClient {

    private final RestTemplate restTemplate;
    
    @Value("${client.battle.base-url:http://battle-service}")
    private String battleServiceBaseUrl;
    
    @Value("${client.battle.internal-auth-token}")
    private String internalAuthToken;

    /**
     * Battle Service에서 댓글 단건 조회
     * 방문 인증 시 댓글 존재 여부와 Battle 정보 확인용
     * 
     * @param commentId 댓글 ID
     * @return 댓글 정보
     */
    public BattleCommentResponse getComment(Long commentId) {
        String url = String.format("%s/api/v1/battles/comments/%d", battleServiceBaseUrl, commentId);
        
        try {
            log.info("Battle Service 댓글 조회 요청: commentId={}", commentId);
            
            // 내부 서비스 인증 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Auth", internalAuthToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ApiResponse<BattleCommentResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                ApiResponse.class
            ).getBody();
            
            if (response == null || !response.isSuccess()) {
                log.warn("Battle Service 응답 오류: commentId={}, response={}", commentId, response);
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_BAD_RESPONSE);
            }
            
            // API 응답에서 data 필드 추출
            Object data = response.getData();
            if (data == null) {
                log.warn("Battle Service 응답 data 없음: commentId={}", commentId);
                throw new CustomException(ErrorCode.VISIT_CERT_COMMENT_NOT_FOUND);
            }
            
            // LinkedHashMap을 BattleCommentResponse로 변환 (RestTemplate 기본 동작)
            if (data instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data;
                
                return BattleCommentResponse.builder()
                    .commentId(((Number) dataMap.get("commentId")).longValue())
                    .battleId(((Number) dataMap.get("battleId")).longValue())
                    .memberId(((Number) dataMap.get("memberId")).longValue())
                    .createdAt(LocalDateTime.parse((String) dataMap.get("createdAt")))
                    .build();
            }
            
            log.warn("Battle Service 응답 타입 오류: commentId={}, dataType={}", commentId, data.getClass());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_BAD_RESPONSE);
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("댓글 없음: commentId={}", commentId);
                throw new CustomException(ErrorCode.VISIT_CERT_COMMENT_NOT_FOUND);
            } else {
                log.error("Battle Service HTTP 오류: commentId={}, status={}, message={}", 
                         commentId, e.getStatusCode(), e.getMessage());
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }
        } catch (ResourceAccessException e) {
            log.error("Battle Service 연결 오류: commentId={}, message={}", commentId, e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        } catch (CustomException e) {
            // CustomException은 그대로 재전파
            throw e;
        } catch (Exception e) {
            log.error("Battle Service 호출 중 예상치 못한 오류: commentId={}", commentId, e);
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    /**
     * 진행 중인 배틀 수 조회 (ACTIVE 상태)
     * 연결 불가 시 null 반환
     */
    public Integer getActiveBattlesCount() {
        String url = String.format("%s/api/v1/battles/internal/active-count", battleServiceBaseUrl);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Auth", internalAuthToken);
            ApiResponse<Object> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), ApiResponse.class).getBody();
            if (response == null || !response.isSuccess()) return null;
            Object data = response.getData();
            return data != null ? ((Number) data).intValue() : null;
        } catch (Exception e) {
            log.warn("Battle 활성 카운트 조회 실패 (null 반환): {}", e.getMessage());
            return null;
        }
    }

    /**
     * Battle 정보 조회 (지역 정보 확인용)
     */
    public BattleResponse getBattle(Long battleId) {
        String url = String.format("%s/api/v1/battles/%d", battleServiceBaseUrl, battleId);
        
        try {
            log.info("Battle Service Battle 조회 요청: battleId={}", battleId);
            
            // 내부 서비스 인증 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Auth", internalAuthToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ApiResponse<BattleResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                ApiResponse.class
            ).getBody();
            
            if (response == null || !response.isSuccess()) {
                log.warn("Battle Service 응답 오류: battleId={}, response={}", battleId, response);
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_BAD_RESPONSE);
            }
            
            Object data = response.getData();
            if (data == null) {
                log.warn("Battle Service 응답 data 없음: battleId={}", battleId);
                throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            
            // LinkedHashMap을 BattleResponse로 변환
            if (data instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data;
                
                return BattleResponse.builder()
                    .battleId(((Number) dataMap.get("battleId")).longValue())
                    .title((String) dataMap.get("title"))
                    .sido((String) dataMap.get("sido"))
                    .sigu((String) dataMap.get("sigu"))
                    .status((String) dataMap.get("status"))
                    .isClosed((Boolean) dataMap.getOrDefault("isClosed", false))
                    .optionA((String) dataMap.get("optionA"))
                    .optionB((String) dataMap.get("optionB"))
                    .build();
            }
            
            log.warn("Battle Service 응답 타입 오류: battleId={}, dataType={}", battleId, data.getClass());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_BAD_RESPONSE);
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Battle 없음: battleId={}", battleId);
                throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
            } else {
                log.error("Battle Service HTTP 오류: battleId={}, status={}, message={}", 
                         battleId, e.getStatusCode(), e.getMessage());
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }
        } catch (ResourceAccessException e) {
            log.error("Battle Service 연결 오류: battleId={}, message={}", battleId, e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        } catch (CustomException e) {
            // CustomException은 그대로 재전파
            throw e;
        } catch (Exception e) {
            log.error("Battle Service 호출 중 예상치 못한 오류: battleId={}", battleId, e);
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    /**
     * Battle 정보 조회 (AI 분석용)
     * 내부 API를 통해 상세 정보 조회
     * 
     * @param battleId Battle ID
     * @return Battle 정보
     */
    public BattleResponse getBattleInfo(Long battleId) {
        String url = String.format("%s/api/v1/battles/%d/info", battleServiceBaseUrl, battleId);
        
        try {
            log.info("Battle Service Battle 내부 정보 조회 요청: battleId={}", battleId);
            
            // 내부 서비스 인증 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Auth", internalAuthToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ApiResponse<BattleResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                ApiResponse.class
            ).getBody();
            
            if (response == null || !response.isSuccess()) {
                log.warn("Battle Service 응답 오류: battleId={}, response={}", battleId, response);
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_BAD_RESPONSE);
            }
            
            Object data = response.getData();
            if (data == null) {
                log.warn("Battle Service 응답 data 없음: battleId={}", battleId);
                throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            
            // LinkedHashMap을 BattleResponse로 변환
            if (data instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data;
                
                return BattleResponse.builder()
                    .battleId(((Number) dataMap.get("battleId")).longValue())
                    .title((String) dataMap.get("title"))
                    .sido((String) dataMap.get("sido"))
                    .sigu((String) dataMap.get("sigu"))
                    .status((String) dataMap.get("status"))
                    .isClosed((Boolean) dataMap.getOrDefault("isClosed", false))
                    .optionA((String) dataMap.get("optionA"))
                    .optionB((String) dataMap.get("optionB"))
                    .build();
            }
            
            log.warn("Battle Service 응답 타입 오류: battleId={}, dataType={}", battleId, data.getClass());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_BAD_RESPONSE);
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Battle 없음: battleId={}", battleId);
                throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
            } else if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.warn("Battle 내부 정보 접근 권한 없음: battleId={}", battleId);
                throw new CustomException(ErrorCode.FORBIDDEN);
            } else {
                log.error("Battle Service HTTP 오류: battleId={}, status={}, message={}", 
                         battleId, e.getStatusCode(), e.getMessage());
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }
        } catch (ResourceAccessException e) {
            log.error("Battle Service 연결 오류: battleId={}, message={}", battleId, e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        } catch (CustomException e) {
            // CustomException은 그대로 재전파
            throw e;
        } catch (Exception e) {
            log.error("Battle Service 호출 중 예상치 못한 오류: battleId={}", battleId, e);
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    /**
     * Battle 투표 원본 데이터 조회 (AI 분석용)
     * 
     * @param battleId Battle ID
     * @return 투표 원본 데이터
     */
    public BattleVotesRawResponse getBattleVotesRaw(Long battleId) {
        String url = String.format("%s/api/v1/battles/%d/votes/raw", battleServiceBaseUrl, battleId);
        
        try {
            log.info("Battle Service 투표 원본 데이터 조회 요청: battleId={}", battleId);
            
            // 내부 서비스 인증 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Auth", internalAuthToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ApiResponse<BattleVotesRawResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                ApiResponse.class
            ).getBody();
            
            if (response == null || !response.isSuccess()) {
                log.warn("Battle Service 응답 오류: battleId={}, response={}", battleId, response);
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_BAD_RESPONSE);
            }
            
            Object data = response.getData();
            if (data == null) {
                log.warn("Battle Service 응답 data 없음: battleId={}", battleId);
                throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            
            // LinkedHashMap을 BattleVotesRawResponse로 변환
            if (data instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data;
                
                @SuppressWarnings("unchecked")
                java.util.List<java.util.Map<String, Object>> votesList = 
                    (java.util.List<java.util.Map<String, Object>>) dataMap.get("votes");
                
                java.util.List<BattleVote> votes = votesList.stream()
                    .map(voteMap -> BattleVote.builder()
                        .memberId(((Number) voteMap.get("memberId")).longValue())
                        .selectedOption((String) voteMap.get("selectedOption"))
                        .votedAt(LocalDateTime.parse((String) voteMap.get("votedAt")))
                        .build())
                    .collect(java.util.stream.Collectors.toList());
                
                return BattleVotesRawResponse.builder()
                    .battleId(((Number) dataMap.get("battleId")).longValue())
                    .votes(votes)
                    .totalVotes(((Number) dataMap.get("totalVotes")).intValue())
                    .build();
            }
            
            log.warn("Battle Service 응답 타입 오류: battleId={}, dataType={}", battleId, data.getClass());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_BAD_RESPONSE);
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Battle 없음: battleId={}", battleId);
                throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
            } else if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.warn("Battle 투표 데이터 접근 권한 없음: battleId={}", battleId);
                throw new CustomException(ErrorCode.FORBIDDEN);
            } else {
                log.error("Battle Service HTTP 오류: battleId={}, status={}, message={}", 
                         battleId, e.getStatusCode(), e.getMessage());
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }
        } catch (ResourceAccessException e) {
            log.error("Battle Service 연결 오류: battleId={}, message={}", battleId, e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        } catch (CustomException e) {
            // CustomException은 그대로 재전파
            throw e;
        } catch (Exception e) {
            log.error("Battle Service 호출 중 예상치 못한 오류: battleId={}", battleId, e);
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }
}
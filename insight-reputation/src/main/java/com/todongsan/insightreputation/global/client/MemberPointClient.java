package com.todongsan.insightreputation.global.client;

import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import com.todongsan.insightreputation.global.response.ApiResponse;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberPointClient {

    private final RestTemplate restTemplate;
    
    @Value("${client.member-point.base-url:http://member-point-service}")
    private String memberPointServiceBaseUrl;

    /**
     * Point 차감
     * 
     * @param memberId 회원 ID
     * @param amount 차감할 포인트
     * @param idempotencyKey 멱등성 키
     */
    public void spendPoints(Long memberId, int amount, String idempotencyKey) {
        String url = String.format("%s/api/v1/points/spend", memberPointServiceBaseUrl);
        
        try {
            log.info("Member-Point Service 포인트 차감 요청: memberId={}, amount={}, idempotencyKey={}", 
                    memberId, amount, idempotencyKey);
            
            // Request Body 생성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("memberId", memberId);
            requestBody.put("amount", amount);
            requestBody.put("reason", "AI 분석 리포트 생성");
            
            // Headers 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Idempotency-Key", idempotencyKey);
            headers.set("Content-Type", "application/json");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ApiResponse<Object> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                request, 
                ApiResponse.class
            ).getBody();
            
            if (response == null || !response.isSuccess()) {
                log.warn("Member-Point Service 응답 오류: memberId={}, response={}", memberId, response);
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_BAD_RESPONSE);
            }
            
            log.info("포인트 차감 성공: memberId={}, amount={}", memberId, amount);
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                String responseBody = e.getResponseBodyAsString();
                if (responseBody.contains("POINT_INSUFFICIENT")) {
                    log.warn("포인트 부족: memberId={}, amount={}", memberId, amount);
                    throw new CustomException(ErrorCode.POINT_INSUFFICIENT);
                } else {
                    log.error("Member-Point Service 요청 오류: memberId={}, status={}, body={}", 
                             memberId, e.getStatusCode(), responseBody);
                    throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
                }
            } else {
                log.error("Member-Point Service HTTP 오류: memberId={}, status={}, message={}", 
                         memberId, e.getStatusCode(), e.getMessage());
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }
        } catch (ResourceAccessException e) {
            log.error("Member-Point Service 연결 오류: memberId={}, message={}", memberId, e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        } catch (CustomException e) {
            // CustomException은 그대로 재전파
            throw e;
        } catch (Exception e) {
            log.error("Member-Point Service 호출 중 예상치 못한 오류: memberId={}", memberId, e);
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    /**
     * Point 환불 (AI 분석 실패 시)
     * 
     * @param memberId 회원 ID
     * @param amount 환불할 포인트
     * @param reason 환불 사유
     */
    public void refundPoints(Long memberId, int amount, String reason) {
        String url = String.format("%s/api/v1/points/refund", memberPointServiceBaseUrl);
        
        try {
            log.info("Member-Point Service 포인트 환불 요청: memberId={}, amount={}, reason={}", 
                    memberId, amount, reason);
            
            // Request Body 생성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("memberId", memberId);
            requestBody.put("amount", amount);
            requestBody.put("reason", reason);
            
            // Headers 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ApiResponse<Object> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                request, 
                ApiResponse.class
            ).getBody();
            
            if (response == null || !response.isSuccess()) {
                log.warn("Member-Point Service 환불 응답 오류: memberId={}, response={}", memberId, response);
                // 환불 실패는 로그만 기록하고 예외를 발생시키지 않음 (비즈니스 연속성)
                return;
            }
            
            log.info("포인트 환불 성공: memberId={}, amount={}, reason={}", memberId, amount, reason);
            
        } catch (Exception e) {
            log.error("Member-Point Service 환불 중 오류 (비즈니스 연속성 유지): memberId={}, amount={}, reason={}", 
                     memberId, amount, reason, e);
            // 환불 실패는 예외를 발생시키지 않음
        }
    }

    /**
     * 회원 정보 배치 조회 (AI 분석용)
     * 
     * @param memberIds 회원 ID 목록
     * @return 회원 정보 목록
     */
    public List<MemberInfoResponse> getBatchMemberInfo(List<Long> memberIds) {
        String url = String.format("%s/api/v1/members/batch", memberPointServiceBaseUrl);
        
        try {
            log.info("Member-Point Service 회원 정보 배치 조회: memberCount={}", memberIds.size());
            
            // Request Body 생성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("memberIds", memberIds);
            
            // Headers 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ApiResponse<List<MemberInfoResponse>> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                request, 
                ApiResponse.class
            ).getBody();
            
            if (response == null || !response.isSuccess()) {
                log.warn("Member-Point Service 응답 오류: memberIds={}, response={}", memberIds, response);
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_BAD_RESPONSE);
            }
            
            Object data = response.getData();
            if (data == null) {
                log.warn("Member-Point Service 응답 data 없음: memberIds={}", memberIds);
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_BAD_RESPONSE);
            }
            
            // LinkedHashMap 리스트를 MemberInfoResponse 리스트로 변환
            if (data instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<java.util.Map<String, Object>> memberList = 
                    (java.util.List<java.util.Map<String, Object>>) data;
                
                return memberList.stream()
                    .map(memberMap -> MemberInfoResponse.builder()
                        .memberId(((Number) memberMap.get("memberId")).longValue())
                        .ageGroup((String) memberMap.get("ageGroup"))
                        .gender((String) memberMap.get("gender"))
                        .residenceSido((String) memberMap.get("residenceSido"))
                        .residenceSigu((String) memberMap.get("residenceSigu"))
                        .build())
                    .collect(java.util.stream.Collectors.toList());
            }
            
            log.warn("Member-Point Service 응답 타입 오류: memberIds={}, dataType={}", memberIds, data.getClass());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_BAD_RESPONSE);
            
        } catch (HttpClientErrorException e) {
            log.error("Member-Point Service HTTP 오류: memberIds={}, status={}, message={}", 
                     memberIds, e.getStatusCode(), e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        } catch (ResourceAccessException e) {
            log.error("Member-Point Service 연결 오류: memberIds={}, message={}", memberIds, e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        } catch (CustomException e) {
            // CustomException은 그대로 재전파
            throw e;
        } catch (Exception e) {
            log.error("Member-Point Service 호출 중 예상치 못한 오류: memberIds={}", memberIds, e);
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }
}
package com.todongsan.battle_service.client;

import com.todongsan.battle_service.client.dto.PointEarnRequest;
import com.todongsan.battle_service.client.dto.PointSettleRequest;
import com.todongsan.battle_service.client.dto.PointSpendRequest;
import com.todongsan.battle_service.global.exception.CustomException;
import com.todongsan.battle_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberPointClient {

    private final RestTemplate restTemplate;

    @Value("${external.member-point.url}")
    private String memberPointUrl;

    public void earnPoint(PointEarnRequest request) {
        String url = memberPointUrl + "/internal/api/v1/points/earn";
        try {
            restTemplate.postForObject(url, buildHttpEntity(request, request.getIdempotencyKey()), Void.class);
        } catch (HttpClientErrorException e) {
            // 4xx: 재시도 불필요, 즉시 예외 전파
            log.warn("Member-Point earn failed (4xx): {}", e.getMessage());
            throw new CustomException(ErrorCode.POINT_INSUFFICIENT);
        } catch (ResourceAccessException e) {
            // Timeout / 연결 실패 → 호출자에서 RetryQueue 적재
            log.warn("Member-Point earn timeout: {}", e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_TIMEOUT);
        }
    }

    public void spendPoint(PointSpendRequest request) {
        String url = memberPointUrl + "/internal/api/v1/points/spend";
        try {
            restTemplate.postForObject(url, buildHttpEntity(request, request.getIdempotencyKey()), Void.class);
        } catch (HttpClientErrorException e) {
            log.warn("Member-Point spend failed (4xx): {}", e.getMessage());
            throw new CustomException(ErrorCode.POINT_INSUFFICIENT);
        } catch (ResourceAccessException e) {
            log.warn("Member-Point spend timeout: {}", e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_TIMEOUT);
        }
    }

    public void settlePoints(List<PointSettleRequest> requests) {
        String url = memberPointUrl + "/internal/api/v1/points/settlements";
        try {
            restTemplate.postForObject(url, buildHttpEntity(requests, null), Void.class);
        } catch (HttpClientErrorException e) {
            log.warn("Member-Point settle failed (4xx): {}", e.getMessage());
            throw new CustomException(ErrorCode.POINT_INSUFFICIENT);
        } catch (ResourceAccessException e) {
            log.warn("Member-Point settle timeout: {}", e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_TIMEOUT);
        }
    }

    private <T> HttpEntity<T> buildHttpEntity(T body, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) {
            headers.set("Idempotency-Key", idempotencyKey);
        }
        return new HttpEntity<>(body, headers);
    }
}

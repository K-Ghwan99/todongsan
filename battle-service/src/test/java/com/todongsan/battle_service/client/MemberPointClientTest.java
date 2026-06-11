package com.todongsan.battle_service.client;

import com.todongsan.battle_service.client.dto.PointEarnRequest;
import com.todongsan.battle_service.global.exception.CustomException;
import com.todongsan.battle_service.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MemberPointClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private MemberPointClient memberPointClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(memberPointClient, "memberPointUrl", "http://test-member-point");
    }

    private PointEarnRequest earnRequest() {
        return PointEarnRequest.builder()
                .memberId(1L)
                .type("EARN_VOTE")
                .referenceType("BATTLE")
                .referenceId(10L)
                .amount(BigDecimal.TEN)
                .idempotencyKey("battle:vote:10:member:1")
                .build();
    }

    @Test
    @DisplayName("earnPoint - Member-Point 5xx 응답 → 재시도 가능 예외(EXTERNAL_SERVICE_TIMEOUT)")
    void earnPoint_serverError5xx_throwsRetryableTimeout() {
        given(restTemplate.postForObject(any(String.class), any(), eq(Void.class)))
                .willThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> memberPointClient.earnPoint(earnRequest()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_SERVICE_TIMEOUT);
    }

    @Test
    @DisplayName("earnPoint - 타임아웃/연결 실패 → 재시도 가능 예외(EXTERNAL_SERVICE_TIMEOUT)")
    void earnPoint_timeout_throwsRetryableTimeout() {
        given(restTemplate.postForObject(any(String.class), any(), eq(Void.class)))
                .willThrow(new ResourceAccessException("connection refused"));

        assertThatThrownBy(() -> memberPointClient.earnPoint(earnRequest()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_SERVICE_TIMEOUT);
    }

    @Test
    @DisplayName("earnPoint - 4xx 응답 → 재시도 불필요 예외(POINT_INSUFFICIENT)")
    void earnPoint_clientError4xx_throwsPointInsufficient() {
        given(restTemplate.postForObject(any(String.class), any(), eq(Void.class)))
                .willThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> memberPointClient.earnPoint(earnRequest()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.POINT_INSUFFICIENT);
    }

    @Test
    @DisplayName("earnPoint - 정상 응답 → 예외 없음")
    void earnPoint_success_noException() {
        given(restTemplate.postForObject(any(String.class), any(), eq(Void.class)))
                .willReturn(null);

        memberPointClient.earnPoint(earnRequest());
        // 예외가 발생하지 않으면 성공
        assertThat(true).isTrue();
    }
}

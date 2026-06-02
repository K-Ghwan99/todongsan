package com.todongsan.insightreputation.global.response;

import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    @DisplayName("성공 응답 생성 시 success=true, errorCode=null, data 포함")
    void ok_withData_returnsSuccessResponse() {
        // given
        String testData = "test data";

        // when
        ApiResponse<String> response = ApiResponse.ok(testData);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getData()).isEqualTo(testData);
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getTimestamp()).isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("성공 응답(데이터 없음) 생성 시 success=true, data=null")
    void success_withoutData_returnsSuccessResponse() {
        // when
        ApiResponse<Void> response = ApiResponse.success();

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("에러 응답 생성 시 success=false, errorCode 포함, data=null")
    void fail_withErrorCode_returnsErrorResponse() {
        // given
        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;

        // when
        ApiResponse<Void> response = ApiResponse.fail(errorCode);

        // then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo(errorCode.getCode());
        assertThat(response.getMessage()).isEqualTo(errorCode.getMessage());
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("에러 응답 생성 시 custom message로 덮어쓰기")
    void fail_withErrorCodeAndCustomMessage_returnsErrorResponseWithCustomMessage() {
        // given
        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
        String customMessage = "사용자 정의 에러 메시지";

        // when
        ApiResponse<Void> response = ApiResponse.fail(errorCode, customMessage);

        // then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo(errorCode.getCode());
        assertThat(response.getMessage()).isEqualTo(customMessage);
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("success(T data) 메서드는 ok(T data)와 동일하게 동작")
    void success_withData_behavesLikeOk() {
        // given
        String testData = "test data";

        // when
        ApiResponse<String> okResponse = ApiResponse.ok(testData);
        ApiResponse<String> successResponse = ApiResponse.success(testData);

        // then
        assertThat(successResponse.isSuccess()).isEqualTo(okResponse.isSuccess());
        assertThat(successResponse.getData()).isEqualTo(okResponse.getData());
        assertThat(successResponse.getErrorCode()).isEqualTo(okResponse.getErrorCode());
        assertThat(successResponse.getMessage()).isEqualTo(okResponse.getMessage());
    }
}
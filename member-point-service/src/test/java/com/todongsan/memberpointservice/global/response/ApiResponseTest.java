package com.todongsan.memberpointservice.global.response;

import com.todongsan.memberpointservice.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void ok_데이터만_있을때_success_true_errorCode_null() {
        ApiResponse<String> response = ApiResponse.ok("data");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("data");
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void ok_메시지_포함시_message_설정됨() {
        ApiResponse<String> response = ApiResponse.ok("data", "처리 완료");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("data");
        assertThat(response.getMessage()).isEqualTo("처리 완료");
    }

    @Test
    void fail_에러코드_있을때_success_false_code_message_일치() {
        ApiResponse<Void> response = ApiResponse.fail(ErrorCode.POINT_INSUFFICIENT);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("POINT_INSUFFICIENT");
        assertThat(response.getMessage()).isEqualTo(ErrorCode.POINT_INSUFFICIENT.getMessage());
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void fail_detail_있을때_message_오버라이드됨() {
        ApiResponse<Void> response = ApiResponse.fail(ErrorCode.POINT_INSUFFICIENT, "현재 잔액: 50P");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("POINT_INSUFFICIENT");
        assertThat(response.getMessage()).isEqualTo("현재 잔액: 50P");
    }
}

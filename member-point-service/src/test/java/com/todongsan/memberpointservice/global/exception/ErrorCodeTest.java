package com.todongsan.memberpointservice.global.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    void 각_에러코드는_code_message_httpStatus를_가진다() {
        for (ErrorCode errorCode : ErrorCode.values()) {
            assertThat(errorCode.getCode()).isNotBlank();
            assertThat(errorCode.getMessage()).isNotBlank();
            assertThat(errorCode.getHttpStatus()).isNotNull();
        }
    }

    @Test
    void POINT_INSUFFICIENT_는_409() {
        assertThat(ErrorCode.POINT_INSUFFICIENT.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void POINT_TRANSACTION_ALREADY_PROCESSED_는_200() {
        assertThat(ErrorCode.POINT_TRANSACTION_ALREADY_PROCESSED.getHttpStatus()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void MEMBER_NOT_FOUND_는_404() {
        assertThat(ErrorCode.MEMBER_NOT_FOUND.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void INTERNAL_ERROR_는_500() {
        assertThat(ErrorCode.INTERNAL_ERROR.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void code_필드는_enum_name과_일치한다() {
        for (ErrorCode errorCode : ErrorCode.values()) {
            assertThat(errorCode.getCode()).isEqualTo(errorCode.name());
        }
    }
}

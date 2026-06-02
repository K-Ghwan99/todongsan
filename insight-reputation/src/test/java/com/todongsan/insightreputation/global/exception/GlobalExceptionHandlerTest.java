package com.todongsan.insightreputation.global.exception;

import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import com.todongsan.insightreputation.global.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    @DisplayName("CustomException 처리 시 올바른 HTTP 상태 코드와 errorCode 반환")
    void handleCustomException_returnsCorrectHttpStatusAndErrorCode() {
        // given
        ErrorCode errorCode = ErrorCode.REPUTATION_NOT_FOUND;
        CustomException exception = new CustomException(errorCode);

        // when
        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleCustomException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(errorCode.getHttpStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo(errorCode.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo(errorCode.getMessage());
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("MethodArgumentNotValidException 처리 시 400 VALIDATION_FAILED 반환")
    void handleMethodArgumentNotValidException_returns400ValidationFailed() {
        // given
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testObject");
        bindingResult.addError(new FieldError("testObject", "testField", "필드가 유효하지 않습니다"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        // when
        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleMethodArgumentNotValidException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("필드가 유효하지 않습니다");
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("예상치 못한 Exception 처리 시 500 INTERNAL_ERROR 반환")
    void handleException_returns500InternalError() {
        // given
        Exception exception = new RuntimeException("예상치 못한 오류");

        // when
        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.INTERNAL_ERROR.getMessage());
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("다양한 ErrorCode에 대해 올바른 HTTP 상태 코드 매핑")
    void handleCustomException_mapsCorrectHttpStatusForDifferentErrorCodes() {
        // given & when & then
        testErrorCodeMapping(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        testErrorCodeMapping(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        testErrorCodeMapping(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        testErrorCodeMapping(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
        testErrorCodeMapping(ErrorCode.DUPLICATE_RESOURCE, HttpStatus.CONFLICT);
        testErrorCodeMapping(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        testErrorCodeMapping(ErrorCode.SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
        testErrorCodeMapping(ErrorCode.EXTERNAL_SERVICE_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT);
    }

    private void testErrorCodeMapping(ErrorCode errorCode, HttpStatus expectedStatus) {
        // given
        CustomException exception = new CustomException(errorCode);

        // when
        ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleCustomException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody().getErrorCode()).isEqualTo(errorCode.getCode());
    }
}
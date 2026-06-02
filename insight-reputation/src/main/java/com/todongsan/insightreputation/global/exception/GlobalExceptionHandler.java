package com.todongsan.insightreputation.global.exception;

import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import com.todongsan.insightreputation.global.response.ApiResponse;
import com.todongsan.insightreputation.reputation.dto.response.ResidenceCooldownErrorResponse;
import com.todongsan.insightreputation.reputation.exception.ResidenceChangeCooldownException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResidenceChangeCooldownException.class)
    public ResponseEntity<ApiResponse<ResidenceCooldownErrorResponse>> handleResidenceChangeCooldownException(ResidenceChangeCooldownException e) {
        log.warn("ResidenceChangeCooldownException occurred: nextChangeAvailableDate={}", e.getNextChangeAvailableDate());
        
        ResidenceCooldownErrorResponse errorData = ResidenceCooldownErrorResponse.builder()
                .nextChangeAvailableDate(e.getNextChangeAvailableDate())
                .build();
        
        ApiResponse<ResidenceCooldownErrorResponse> response = ApiResponse.<ResidenceCooldownErrorResponse>builder()
                .success(false)
                .errorCode(e.getErrorCode().getCode())
                .message(e.getErrorCode().getMessage())
                .data(errorData)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        log.warn("CustomException occurred: {} - {}", e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.fail(e.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getBindingResult().getAllErrors().get(0).getDefaultMessage());
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity
                .status(ErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.VALIDATION_FAILED, errorMessage));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected exception occurred", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR));
    }
}
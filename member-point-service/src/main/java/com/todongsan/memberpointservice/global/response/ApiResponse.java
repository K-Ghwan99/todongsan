package com.todongsan.memberpointservice.global.response;

import com.todongsan.memberpointservice.global.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 공통 응답 포맷
@Getter
@Builder
public class ApiResponse<T> {

    private final boolean success;
    private final String errorCode;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    // 성공 응답
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 성공 응답 (메시지 포함)
    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 실패 응답
    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return ApiResponse.<Void>builder()
                .success(false)
                .errorCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 실패 응답 (메시지 오버라이드)
    public static ApiResponse<Void> fail(ErrorCode errorCode, String detail) {
        return ApiResponse.<Void>builder()
                .success(false)
                .errorCode(errorCode.getCode())
                .message(detail)
                .timestamp(LocalDateTime.now())
                .build();
    }



}

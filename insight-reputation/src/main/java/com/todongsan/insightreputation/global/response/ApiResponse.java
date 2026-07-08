package com.todongsan.insightreputation.global.response;

import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApiResponse<T> {

    private final boolean success;
    private final String errorCode;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return ok(data);
    }

    public static ApiResponse<Void> success() {
        return ApiResponse.<Void>builder()
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return ApiResponse.<Void>builder()
                .success(false)
                .errorCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, String detail) {
        return ApiResponse.<Void>builder()
                .success(false)
                .errorCode(errorCode.getCode())
                .message(detail)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
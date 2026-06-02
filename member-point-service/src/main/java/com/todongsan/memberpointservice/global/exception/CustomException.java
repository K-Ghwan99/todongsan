package com.todongsan.memberpointservice.global.exception;

import lombok.Getter;

// ErrorCode를 래핑하는 런타임 예외
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}

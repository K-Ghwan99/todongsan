package com.todongsan.marketservice.global.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MarketErrorCode implements ErrorCode {

    MARKET_NOT_FOUND("MARKET_NOT_FOUND", "Market을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}

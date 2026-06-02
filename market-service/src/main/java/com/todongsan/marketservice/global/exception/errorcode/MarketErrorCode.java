package com.todongsan.marketservice.global.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MarketErrorCode implements ErrorCode {

    MARKET_NOT_ACTIVE("MARKET_NOT_ACTIVE", "예측 참여 가능한 상태의 Market이 아닙니다.", HttpStatus.CONFLICT),
    MARKET_ALREADY_PREDICTED("MARKET_ALREADY_PREDICTED", "이미 예측 참여한 Market입니다.", HttpStatus.CONFLICT),
    MARKET_OPTION_NOT_FOUND("MARKET_OPTION_NOT_FOUND", "Market 선택지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    MARKET_INVALID_BET_AMOUNT("MARKET_INVALID_BET_AMOUNT", "예측 참여 포인트 금액이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    MARKET_PRICE_UPDATE_CONFLICT("MARKET_PRICE_UPDATE_CONFLICT", "Market 가격 확정 트랜잭션 중 동시성 충돌이 발생했습니다.", HttpStatus.CONFLICT),
    MARKET_CLOSED("MARKET_CLOSED", "이미 마감된 Market입니다.", HttpStatus.CONFLICT),
    MARKET_INVALID_STATUS("MARKET_INVALID_STATUS", "현재 Market 상태에서는 요청한 작업을 수행할 수 없습니다.", HttpStatus.CONFLICT),
    MARKET_NOT_FOUND("MARKET_NOT_FOUND", "Market을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    MARKET_INVALID_OPTION("MARKET_INVALID_OPTION", "Market 선택지 구성이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    MARKET_INVALID_OPTION_RANGE("MARKET_INVALID_OPTION_RANGE", "Market 선택지 범위가 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    MARKET_INVALID_FEE_RATE("MARKET_INVALID_FEE_RATE", "Market 수수료율이 유효하지 않습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}

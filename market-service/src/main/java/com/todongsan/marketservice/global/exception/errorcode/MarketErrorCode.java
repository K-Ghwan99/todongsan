package com.todongsan.marketservice.global.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MarketErrorCode implements ErrorCode {

    MARKET_NOT_ACTIVE("MARKET_NOT_ACTIVE", "예측 참여 가능한 상태의 Market이 아닙니다.", HttpStatus.CONFLICT),
    MARKET_ALREADY_PREDICTED("MARKET_ALREADY_PREDICTED", "이미 예측 참여한 Market입니다.", HttpStatus.CONFLICT),
    MARKET_PREDICTION_NOT_FOUND("MARKET_PREDICTION_NOT_FOUND", "내 예측을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    MARKET_OPTION_NOT_FOUND("MARKET_OPTION_NOT_FOUND", "Market 선택지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    MARKET_INVALID_BET_AMOUNT("MARKET_INVALID_BET_AMOUNT", "예측 참여 포인트 금액이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    MARKET_PRICE_UPDATE_CONFLICT("MARKET_PRICE_UPDATE_CONFLICT", "Market 가격 확정 트랜잭션 중 동시성 충돌이 발생했습니다.", HttpStatus.CONFLICT),
    MARKET_CLOSED("MARKET_CLOSED", "이미 마감된 Market입니다.", HttpStatus.CONFLICT),
    MARKET_COMMENT_TOO_LONG("MARKET_COMMENT_TOO_LONG", "Market 댓글은 500자를 초과할 수 없습니다.", HttpStatus.BAD_REQUEST),
    MARKET_COMMENT_NOT_FOUND("MARKET_COMMENT_NOT_FOUND", "Market 댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    MARKET_COMMENT_FORBIDDEN("MARKET_COMMENT_FORBIDDEN", "다른 회원의 Market 댓글은 삭제할 수 없습니다.", HttpStatus.FORBIDDEN),
    MARKET_COMMENT_NOT_ALLOWED("MARKET_COMMENT_NOT_ALLOWED", "현재 Market 상태에서는 댓글을 작성할 수 없습니다.", HttpStatus.CONFLICT),
    MARKET_INVALID_STATUS("MARKET_INVALID_STATUS", "현재 Market 상태에서는 요청한 작업을 수행할 수 없습니다.", HttpStatus.CONFLICT),
    MARKET_NOT_FOUND("MARKET_NOT_FOUND", "Market을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    MARKET_INVALID_OPTION("MARKET_INVALID_OPTION", "Market 선택지 구성이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    MARKET_INVALID_OPTION_RANGE("MARKET_INVALID_OPTION_RANGE", "Market 선택지 범위가 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    MARKET_INVALID_FEE_RATE("MARKET_INVALID_FEE_RATE", "Market 수수료율이 유효하지 않습니다.", HttpStatus.BAD_REQUEST),
    MARKET_WINNING_OPTION_NOT_FOUND("MARKET_WINNING_OPTION_NOT_FOUND", "정산 결과와 매칭되는 정답 선택지를 찾을 수 없습니다.", HttpStatus.CONFLICT),
    MARKET_INVALID_SETTLEMENT_DATA("MARKET_INVALID_SETTLEMENT_DATA", "정산 데이터가 유효하지 않습니다.", HttpStatus.CONFLICT),
    MARKET_ALREADY_SETTLED("MARKET_ALREADY_SETTLED", "이미 정산 완료된 Market입니다.", HttpStatus.CONFLICT),
    MARKET_NO_PREDICTIONS("MARKET_NO_PREDICTIONS", "정산할 예측 참여자가 없습니다.", HttpStatus.CONFLICT),
    MARKET_CANNOT_VOID("MARKET_CANNOT_VOID", "현재 상태에서는 Market을 무효 처리할 수 없습니다.", HttpStatus.CONFLICT),
    MARKET_REFUND_NOT_ALLOWED("MARKET_REFUND_NOT_ALLOWED", "현재 상태에서는 Market 환불을 실행할 수 없습니다.", HttpStatus.CONFLICT),
    MARKET_ALREADY_REFUNDED("MARKET_ALREADY_REFUNDED", "이미 환불 처리가 시작된 Market입니다.", HttpStatus.CONFLICT),
    MARKET_REFUND_FAILED("MARKET_REFUND_FAILED", "Market 환불 처리에 실패했습니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}

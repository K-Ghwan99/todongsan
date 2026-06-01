package com.todongsan.insightreputation.global.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "권한이 없습니다.", HttpStatus.FORBIDDEN),
    NOT_FOUND("NOT_FOUND", "리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // Point
    POINT_INSUFFICIENT("POINT_INSUFFICIENT", "포인트가 부족합니다.", HttpStatus.BAD_REQUEST),

    // Battle
    ALREADY_VOTED("ALREADY_VOTED", "이미 투표한 Battle입니다.", HttpStatus.CONFLICT),
    BATTLE_CLOSED("BATTLE_CLOSED", "종료된 Battle입니다.", HttpStatus.BAD_REQUEST),
    BATTLE_NOT_CLOSED("BATTLE_NOT_CLOSED", "Battle이 아직 종료되지 않았습니다.", HttpStatus.BAD_REQUEST),

    // Market
    ALREADY_PREDICTED("ALREADY_PREDICTED", "이미 참여한 Market입니다.", HttpStatus.CONFLICT),
    MARKET_CLOSED("MARKET_CLOSED", "마감된 Market입니다.", HttpStatus.BAD_REQUEST),
    INVALID_SETTLE("INVALID_SETTLE", "정산 조건을 충족하지 않습니다.", HttpStatus.BAD_REQUEST),

    // Insight-Reputation 특화 에러
    TOO_FAR_FROM_REGION("TOO_FAR_FROM_REGION", "GPS 좌표가 지역 중심에서 3km를 초과합니다.", HttpStatus.BAD_REQUEST),
    COMMENT_REGION_MISMATCH("COMMENT_REGION_MISMATCH", "댓글이 해당 지역 Battle이 아닙니다.", HttpStatus.BAD_REQUEST),
    ALREADY_CERTIFIED_RECENTLY("ALREADY_CERTIFIED_RECENTLY", "동일 지역 30일 내 재인증을 시도했습니다.", HttpStatus.BAD_REQUEST),
    AI_GENERATION_FAILED("AI_GENERATION_FAILED", "AI 분석 생성에 실패했습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
package com.todongsan.battle_service.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "권한이 없습니다.", HttpStatus.FORBIDDEN),
    VALIDATION_FAILED("VALIDATION_FAILED", "요청 값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    POINT_INSUFFICIENT("POINT_INSUFFICIENT", "포인트가 부족합니다.", HttpStatus.BAD_REQUEST),
    IDEMPOTENCY_KEY_REQUIRED("IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key 헤더가 필요합니다.", HttpStatus.BAD_REQUEST),
    EXTERNAL_SERVICE_TIMEOUT("EXTERNAL_SERVICE_TIMEOUT", "외부 서비스 응답 시간이 초과되었습니다.", HttpStatus.GATEWAY_TIMEOUT),
    EXTERNAL_SERVICE_UNAVAILABLE("EXTERNAL_SERVICE_UNAVAILABLE", "외부 서비스를 사용할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE),

    // Battle 도메인
    BATTLE_NOT_FOUND("BATTLE_NOT_FOUND", "존재하지 않는 Battle입니다.", HttpStatus.NOT_FOUND),
    BATTLE_CLOSED("BATTLE_CLOSED", "종료된 Battle입니다.", HttpStatus.CONFLICT),
    BATTLE_ALREADY_VOTED("BATTLE_ALREADY_VOTED", "이미 투표한 Battle입니다.", HttpStatus.CONFLICT),
    BATTLE_INVALID_OPTION("BATTLE_INVALID_OPTION", "올바르지 않은 선택지입니다.", HttpStatus.BAD_REQUEST),
    BATTLE_INVALID_PERIOD("BATTLE_INVALID_PERIOD", "Battle 기간이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    BATTLE_INVALID_STATUS("BATTLE_INVALID_STATUS", "현재 상태에서 처리할 수 없습니다.", HttpStatus.CONFLICT),
    BATTLE_RESULT_NOT_AVAILABLE("BATTLE_RESULT_NOT_AVAILABLE", "진행 중인 Battle은 상세 결과를 볼 수 없습니다.", HttpStatus.CONFLICT),

    // Comment 도메인
    BATTLE_COMMENT_NOT_FOUND("BATTLE_COMMENT_NOT_FOUND", "존재하지 않는 댓글입니다.", HttpStatus.NOT_FOUND),
    BATTLE_COMMENT_FORBIDDEN("BATTLE_COMMENT_FORBIDDEN", "본인 댓글만 삭제할 수 있습니다.", HttpStatus.FORBIDDEN),
    BATTLE_COMMENT_TOO_LONG("BATTLE_COMMENT_TOO_LONG", "댓글 길이가 너무 깁니다. (최대 500자)", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}

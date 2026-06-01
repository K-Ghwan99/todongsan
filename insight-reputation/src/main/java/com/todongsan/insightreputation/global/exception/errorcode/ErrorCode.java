package com.todongsan.insightreputation.global.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // === 공통 에러 코드 (ERROR_POLICY.md) ===
    
    // 요청 형식 관련 (400)
    INVALID_REQUEST("INVALID_REQUEST", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED("VALIDATION_FAILED", "입력값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_PARAMETER("MISSING_REQUIRED_PARAMETER", "필수 파라미터가 없습니다.", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_HEADER("MISSING_REQUIRED_HEADER", "필수 헤더가 없습니다.", HttpStatus.BAD_REQUEST),
    METHOD_ARGUMENT_TYPE_MISMATCH("METHOD_ARGUMENT_TYPE_MISMATCH", "파라미터 타입이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_MEDIA_TYPE("UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 미디어 타입입니다.", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "허용되지 않는 HTTP 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),

    // 인증/인가 관련
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("INVALID_TOKEN", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "권한이 없습니다.", HttpStatus.FORBIDDEN),

    // 리소스 관련
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    API_NOT_FOUND("API_NOT_FOUND", "존재하지 않는 API입니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE("DUPLICATE_RESOURCE", "중복된 리소스입니다.", HttpStatus.CONFLICT),

    // 멱등성 관련
    IDEMPOTENCY_KEY_REQUIRED("IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key 헤더가 필요합니다.", HttpStatus.BAD_REQUEST),
    IDEMPOTENCY_KEY_INVALID("IDEMPOTENCY_KEY_INVALID", "유효하지 않은 Idempotency-Key입니다.", HttpStatus.BAD_REQUEST),
    IDEMPOTENCY_KEY_CONFLICT("IDEMPOTENCY_KEY_CONFLICT", "이미 처리된 요청입니다.", HttpStatus.CONFLICT),

    // 서비스 간 통신 관련
    EXTERNAL_SERVICE_TIMEOUT("EXTERNAL_SERVICE_TIMEOUT", "외부 서비스 응답 시간이 초과되었습니다.", HttpStatus.GATEWAY_TIMEOUT),
    EXTERNAL_SERVICE_UNAVAILABLE("EXTERNAL_SERVICE_UNAVAILABLE", "외부 서비스를 사용할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE),
    EXTERNAL_SERVICE_ERROR("EXTERNAL_SERVICE_ERROR", "외부 서비스에서 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    EXTERNAL_SERVICE_BAD_RESPONSE("EXTERNAL_SERVICE_BAD_RESPONSE", "외부 서비스 응답이 올바르지 않습니다.", HttpStatus.BAD_GATEWAY),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "서비스를 사용할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE),

    // 서버 내부 오류
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_ERROR("DATABASE_ERROR", "데이터베이스 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_TIMEOUT("DATABASE_TIMEOUT", "데이터베이스 응답 시간이 초과되었습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // === 도메인별 에러 코드 (INSIGHT_ERROR_CODE.md) ===

    // Point 관련
    POINT_INSUFFICIENT("POINT_INSUFFICIENT", "포인트가 부족합니다.", HttpStatus.BAD_REQUEST),

    // Battle 관련
    ALREADY_VOTED("ALREADY_VOTED", "이미 투표한 Battle입니다.", HttpStatus.CONFLICT),
    BATTLE_CLOSED("BATTLE_CLOSED", "종료된 Battle입니다.", HttpStatus.BAD_REQUEST),
    BATTLE_NOT_CLOSED("BATTLE_NOT_CLOSED", "Battle이 아직 종료되지 않았습니다.", HttpStatus.BAD_REQUEST),

    // Market 관련
    ALREADY_PREDICTED("ALREADY_PREDICTED", "이미 참여한 Market입니다.", HttpStatus.CONFLICT),
    MARKET_CLOSED("MARKET_CLOSED", "마감된 Market입니다.", HttpStatus.BAD_REQUEST),
    INVALID_SETTLE("INVALID_SETTLE", "정산 조건을 충족하지 않습니다.", HttpStatus.BAD_REQUEST),

    // Reputation 도메인
    REPUTATION_NOT_FOUND("REPUTATION_NOT_FOUND", "신뢰도 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    REPUTATION_ALREADY_EXISTS("REPUTATION_ALREADY_EXISTS", "이미 신뢰도 정보가 존재합니다.", HttpStatus.CONFLICT),
    REPUTATION_RESIDENCE_CHANGE_COOLDOWN("REPUTATION_RESIDENCE_CHANGE_COOLDOWN", "거주지역 변경 쿨다운 중입니다.", HttpStatus.BAD_REQUEST),

    // Visit Certification 도메인
    VISIT_CERT_COOLDOWN("VISIT_CERT_COOLDOWN", "방문 인증 쿨다운 중입니다.", HttpStatus.BAD_REQUEST),
    VISIT_CERT_OUT_OF_RANGE("VISIT_CERT_OUT_OF_RANGE", "현재 위치가 인증 가능 반경을 벗어났습니다.", HttpStatus.BAD_REQUEST),
    VISIT_CERT_UNSUPPORTED_REGION("VISIT_CERT_UNSUPPORTED_REGION", "지원하지 않는 지역입니다.", HttpStatus.BAD_REQUEST),
    VISIT_CERT_COMMENT_NOT_FOUND("VISIT_CERT_COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    VISIT_CERT_COMMENT_REGION_MISMATCH("VISIT_CERT_COMMENT_REGION_MISMATCH", "해당 지역 Battle의 댓글이 아닙니다.", HttpStatus.BAD_REQUEST),

    // Insight Report 도메인
    INSIGHT_REPORT_NOT_FOUND("INSIGHT_REPORT_NOT_FOUND", "분석 리포트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INSIGHT_REPORT_ALREADY_PROCESSING("INSIGHT_REPORT_ALREADY_PROCESSING", "이미 분석이 진행 중입니다.", HttpStatus.CONFLICT),
    INSIGHT_REPORT_SOURCE_NOT_CLOSED("INSIGHT_REPORT_SOURCE_NOT_CLOSED", "투표가 종료된 Battle만 분석할 수 있습니다.", HttpStatus.BAD_REQUEST),
    INSIGHT_REPORT_SOURCE_DATA_NOT_READY("INSIGHT_REPORT_SOURCE_DATA_NOT_READY", "분석에 필요한 데이터가 준비되지 않았습니다.", HttpStatus.CONFLICT),
    INSIGHT_REPORT_GENERATION_FAILED("INSIGHT_REPORT_GENERATION_FAILED", "AI 분석 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
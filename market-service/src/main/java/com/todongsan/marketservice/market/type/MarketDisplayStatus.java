package com.todongsan.marketservice.market.type;

/**
 * 프론트 표시용 상태. DB의 {@link MarketStatus}와 분리된 파생 값이다.
 *
 * <p>{@code CLOSED_BY_TIME}은 DB 상태가 아니라, status=ACTIVE 이지만 closeAt이 지난
 * Market을 화면에서 "마감"으로 표시하기 위한 표시 전용 값이다. DB의 MarketStatus는 변경하지 않는다.
 */
public enum MarketDisplayStatus {
    PENDING,
    ACTIVE,
    CLOSED_BY_TIME,
    DATA_PENDING,
    CLOSED,
    SETTLEMENT_IN_PROGRESS,
    SETTLED,
    VOIDED
}

package com.todongsan.memberpointservice.point.entity;

// 포인트 이력 유형 (방향은 prefix로 구분)
public enum PointHistoryType {

    // 적립
    EARN_SIGNUP,            // 신규 가입 보상
    EARN_VOTE,              // Battle 투표 참여
    EARN_VOTE_WIN,          // Battle 승리 진영 추가 보상
    EARN_COMMENT,           // Battle 댓글 작성
    EARN_BATTLE_APPROVED,   // Battle 주제 등록 승인

    // 차감
    SPEND_MARKET,           // Market 예측 참여
    SPEND_INSIGHT,          // Insight 열람
    SPEND_BATTLE_CREATE,    // Battle 주제 생성권
    SPEND_SLOT,             // 관심 지역 슬롯 확장

    // 정산
    SETTLE_MARKET,          // Market 정산 보상
    REFUND_MARKET,          // Market 무효 환불
    REFUND_INSIGHT,         // AI 리포트 생성 실패 환불

    // 시스템
    BURN                    // 소각 (소수점 처리)
}

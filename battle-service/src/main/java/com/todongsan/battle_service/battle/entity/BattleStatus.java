package com.todongsan.battle_service.battle.entity;

public enum BattleStatus {
    PENDING,    // 검수 대기 (사용자 등록 후 관리자 승인 전)
    ACTIVE,     // 투표 진행 중 (관리자 승인 후)
    CLOSED,     // 투표 종료 (end_at 도달, 자동 전환)
    CANCELLED   // 강제 취소 / 관리자 거절
}

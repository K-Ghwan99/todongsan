package com.todongsan.battle_service.retry.entity;

public enum RetryStatus {
    PENDING,  // 재시도 대기
    SUCCESS,  // 성공
    FAILED    // 최대 재시도 횟수 초과 (관리자 수동 보정)
}

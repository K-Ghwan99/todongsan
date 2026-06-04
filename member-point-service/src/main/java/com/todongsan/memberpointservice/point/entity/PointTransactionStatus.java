package com.todongsan.memberpointservice.point.entity;

public enum PointTransactionStatus {
    SUCCEEDED, // 처리 성공
    FAILED      // 처리 실패 (예: POINT_INSUFFICIENT)
}

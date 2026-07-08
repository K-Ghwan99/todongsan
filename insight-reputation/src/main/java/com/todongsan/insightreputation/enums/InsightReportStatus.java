package com.todongsan.insightreputation.enums;

public enum InsightReportStatus {
    PENDING,        // 트리거 발생, 처리 대기
    PROCESSING,     // Claude API 호출 중
    DONE,           // 분석 완료
    FAILED          // 실패 (failed_reason 참조)
}
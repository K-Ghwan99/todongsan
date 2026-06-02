package com.todongsan.memberpointservice.member.entity;

import com.fasterxml.jackson.annotation.JsonValue;

// 연령대 (카카오 생년 기반 계산)
public enum AgeGroup {
    AGE_10S("10대"),
    AGE_20S("20대"),
    AGE_30S("30대"),
    AGE_40S("40대"),
    AGE_50S_ABOVE("50대 이상"),
    UNKNOWN("UNKNOWN");

    private final String label;

    AgeGroup(String label) {
        this.label = label;
    }

    // JSON 직렬화 시 label 값 사용
    @JsonValue
    public String getLabel() {
        return label;
    }

}

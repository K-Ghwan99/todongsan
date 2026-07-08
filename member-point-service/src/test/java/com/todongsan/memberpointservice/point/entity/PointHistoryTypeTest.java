package com.todongsan.memberpointservice.point.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PointHistoryTypeTest {

    // PointHistoryType 값 개수 — 13개
    @Test
    void values_count_isThirteen() {
        assertThat(PointHistoryType.values()).hasSize(13);
    }

    // PointReferenceType 값 개수 — 3개
    @Test
    void referenceType_values_count_isThree() {
        assertThat(PointReferenceType.values()).hasSize(3);
    }
}

package com.todongsan.memberpointservice.member.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgeGroupTest {

    // 전체 label 검증
    @Test
    void label_allValues_matchExpected() {
        assertThat(AgeGroup.AGE_10S.getLabel()).isEqualTo("10대");
        assertThat(AgeGroup.AGE_20S.getLabel()).isEqualTo("20대");
        assertThat(AgeGroup.AGE_30S.getLabel()).isEqualTo("30대");
        assertThat(AgeGroup.AGE_40S.getLabel()).isEqualTo("40대");
        assertThat(AgeGroup.AGE_50S_ABOVE.getLabel()).isEqualTo("50대 이상");
        assertThat(AgeGroup.UNKNOWN.getLabel()).isEqualTo("UNKNOWN");
    }

    // @JsonValue — JSON 직렬화 시 label 출력
    @Test
    void jsonValue_serialized_asLabel() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        String json = objectMapper.writeValueAsString(AgeGroup.AGE_20S);

        assertThat(json).isEqualTo("\"20대\"");
    }
}

package com.todongsan.insightreputation.publicdata.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RebApiResponseTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    @DisplayName("정상 응답 역직렬화 - SttsApiTblData 래퍼 파싱")
    void parseValidResponse_sttsApiTblDataWrapper_success() throws Exception {
        // given
        String validJsonResponse = """
            {
              "SttsApiTblData": [
                {
                  "head": [
                    {"list_total_count": 166348},
                    {"RESULT": {"CODE": "INFO-000", "MESSAGE": "정상 처리되었습니다."}}
                  ]
                },
                {
                  "row": [
                    {
                      "CLS_ID": 50071,
                      "CLS_FULLNM": "경기>경부1권>과천시",
                      "DTA_VAL": 58.2810985505133,
                      "WRTTIME_DESC": "2012-05-07",
                      "DTACYCLE_CD": "WK"
                    }
                  ]
                }
              ]
            }
            """;
        
        // when
        RebApiResponse response = objectMapper.readValue(validJsonResponse, RebApiResponse.class);
        
        // then
        assertNotNull(response);
        assertEquals("INFO-000", response.getResultCode());
        assertEquals(166348, response.getTotalCount());
        assertEquals(1, response.getRows().size());
        
        RebDataRow dataRow = response.getRows().get(0);
        assertEquals(50071L, dataRow.getClsId());
        assertEquals("경기>경부1권>과천시", dataRow.getClsFullnm());
        assertEquals(58.2810985505133, dataRow.getDtaVal());
        assertEquals("2012-05-07", dataRow.getWrtimeDesc());
        assertEquals("WK", dataRow.getDtaCycleCd());
    }
    
    @Test
    @DisplayName("INFO-200 응답 역직렬화 - 최상위 RESULT 파싱")
    void parseInfo200Response_directResult_success() throws Exception {
        // given
        String info200JsonResponse = """
            {"RESULT": {"CODE": "INFO-200", "MESSAGE": "해당하는 데이터가 없습니다."}}
            """;
        
        // when
        RebApiResponse response = objectMapper.readValue(info200JsonResponse, RebApiResponse.class);
        
        // then
        assertNotNull(response);
        assertEquals("INFO-200", response.getResultCode());
        assertNull(response.getTotalCount());
        assertTrue(response.getRows().isEmpty());
    }
    
    @Test
    @DisplayName("ERROR-290 응답 역직렬화 - 인증 오류")
    void parseError290Response_authenticationError_success() throws Exception {
        // given
        String errorJsonResponse = """
            {"RESULT": {"CODE": "ERROR-290", "MESSAGE": "인증키가 유효하지 않습니다."}}
            """;
        
        // when
        RebApiResponse response = objectMapper.readValue(errorJsonResponse, RebApiResponse.class);
        
        // then
        assertNotNull(response);
        assertEquals("ERROR-290", response.getResultCode());
        assertNull(response.getTotalCount());
        assertTrue(response.getRows().isEmpty());
    }
    
    @Test
    @DisplayName("빈 row 배열 응답 처리")
    void parseEmptyRowResponse_emptyRows_success() throws Exception {
        // given
        String emptyRowResponse = """
            {
              "SttsApiTblData": [
                {
                  "head": [
                    {"list_total_count": 0},
                    {"RESULT": {"CODE": "INFO-000", "MESSAGE": "정상 처리되었습니다."}}
                  ]
                },
                {
                  "row": []
                }
              ]
            }
            """;
        
        // when
        RebApiResponse response = objectMapper.readValue(emptyRowResponse, RebApiResponse.class);
        
        // then
        assertNotNull(response);
        assertEquals("INFO-000", response.getResultCode());
        assertEquals(0, response.getTotalCount());
        assertTrue(response.getRows().isEmpty());
    }
}
package com.todongsan.insightreputation.publicdata.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todongsan.insightreputation.enums.PublicDataType;
import com.todongsan.insightreputation.publicdata.dto.ParsedDataRow;
import com.todongsan.insightreputation.publicdata.dto.RebDataRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RebDataParserTest {
    
    private RebDataParser parser;
    
    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        parser = new RebDataParser(objectMapper);
    }
    
    @Test
    @DisplayName("REB 데이터 파싱 - 정상 데이터 처리 성공")
    void parseRebData_validData_success() throws Exception {
        // given
        RebDataRow rebData = createRebDataRow("1001", "서울>강남구>역삼동", "100.5", "2025.06.01");
        List<RebDataRow> rebDataRows = Arrays.asList(rebData);
        
        // when
        List<ParsedDataRow> result = parser.parseRebData(rebDataRows, PublicDataType.WEEKLY_PRICE_INDEX);
        
        // then
        assertEquals(1, result.size());
        
        ParsedDataRow parsed = result.get(0);
        assertEquals(PublicDataType.WEEKLY_PRICE_INDEX, parsed.getDataType());
        assertEquals(LocalDate.of(2025, 6, 1), parsed.getReferenceDate());
        assertEquals("서울", parsed.getRegionSido());
        assertEquals("1001", parsed.getSourceRegionId());
        assertEquals("서울>강남구>역삼동", parsed.getRegionFullpath());
        assertEquals(new BigDecimal("100.5"), parsed.getNumericValue());
        assertNotNull(parsed.getRawData());
    }
    
    @Test
    @DisplayName("REB 데이터 파싱 - 계층 구조 지역명에서 sido 추출")
    void parseRebData_hierarchicalRegion_extractsSidoCorrectly() throws Exception {
        // given
        RebDataRow rebData1 = createRebDataRow("2001", "부산>해운대구>우동", "98.7", "2025.05.15");
        RebDataRow rebData2 = createRebDataRow("3001", "대구>중구", "102.1", "2025.05.15");
        RebDataRow rebData3 = createRebDataRow("0000", "전국", "100.0", "2025.05.15");
        List<RebDataRow> rebDataRows = Arrays.asList(rebData1, rebData2, rebData3);
        
        // when
        List<ParsedDataRow> result = parser.parseRebData(rebDataRows, PublicDataType.MONTHLY_PRICE_INDEX);
        
        // then
        assertEquals(3, result.size());
        
        assertEquals("부산", result.get(0).getRegionSido());
        assertEquals("대구", result.get(1).getRegionSido());
        assertEquals("전국", result.get(2).getRegionSido());
    }
    
    @Test
    @DisplayName("REB 데이터 파싱 - 필수 필드 누락 시 해당 행 건너뜀")
    void parseRebData_missingRequiredFields_skipsRow() throws Exception {
        // given
        RebDataRow validData = createRebDataRow("1001", "서울>강남구", "100.5", "2025.06.01");
        RebDataRow invalidData1 = createRebDataRow(null, "서울>강남구", "100.5", "2025.06.01"); // clsId null
        RebDataRow invalidData2 = createRebDataRow("1002", null, "100.5", "2025.06.01"); // clsFullnm null
        RebDataRow invalidData3 = createRebDataRow("1003", "서울>강남구", "100.5", null); // wrttimeDesc null
        
        List<RebDataRow> rebDataRows = Arrays.asList(validData, invalidData1, invalidData2, invalidData3);
        
        // when
        List<ParsedDataRow> result = parser.parseRebData(rebDataRows, PublicDataType.WEEKLY_PRICE_INDEX);
        
        // then
        assertEquals(1, result.size()); // 유효한 데이터 1개만 처리
        assertEquals("1001", result.get(0).getSourceRegionId());
    }
    
    @Test
    @DisplayName("REB 데이터 파싱 - 잘못된 날짜 형식 시 해당 행 건너뜀")
    void parseRebData_invalidDateFormat_skipsRow() throws Exception {
        // given
        RebDataRow validData = createRebDataRow("1001", "서울>강남구", "100.5", "2025.06.01");
        RebDataRow invalidDateData = createRebDataRow("1002", "서울>서초구", "99.8", "invalid-date");
        
        List<RebDataRow> rebDataRows = Arrays.asList(validData, invalidDateData);
        
        // when
        List<ParsedDataRow> result = parser.parseRebData(rebDataRows, PublicDataType.WEEKLY_PRICE_INDEX);
        
        // then
        assertEquals(1, result.size()); // 유효한 데이터 1개만 처리
        assertEquals("1001", result.get(0).getSourceRegionId());
    }
    
    @Test
    @DisplayName("REB 데이터 파싱 - 빈 리스트 입력 시 빈 결과 반환")
    void parseRebData_emptyInput_returnsEmptyList() throws Exception {
        // given
        List<RebDataRow> emptyList = Collections.emptyList();
        
        // when
        List<ParsedDataRow> result = parser.parseRebData(emptyList, PublicDataType.WEEKLY_PRICE_INDEX);
        
        // then
        assertTrue(result.isEmpty());
    }
    
    @Test
    @DisplayName("REB 데이터 파싱 - 수치값 null 허용")
    void parseRebData_nullNumericValue_allowsNull() throws Exception {
        // given
        RebDataRow rebData = createRebDataRow("1001", "서울>강남구", null, "2025.06.01");
        List<RebDataRow> rebDataRows = Arrays.asList(rebData);
        
        // when
        List<ParsedDataRow> result = parser.parseRebData(rebDataRows, PublicDataType.WEEKLY_PRICE_INDEX);
        
        // then
        assertEquals(1, result.size());
        assertNull(result.get(0).getNumericValue());
    }
    
    @Test
    @DisplayName("REB 데이터 파싱 - 공백 문자 처리")
    void parseRebData_whitespaceHandling_trimsCorrectly() throws Exception {
        // given
        RebDataRow rebData = createRebDataRow("1001", "  서울>강남구  ", "100.5", "  2025.06.01  ");
        List<RebDataRow> rebDataRows = Arrays.asList(rebData);
        
        // when
        List<ParsedDataRow> result = parser.parseRebData(rebDataRows, PublicDataType.WEEKLY_PRICE_INDEX);
        
        // then
        assertEquals(1, result.size());
        assertEquals("서울", result.get(0).getRegionSido());
        assertEquals(LocalDate.of(2025, 6, 1), result.get(0).getReferenceDate());
    }
    
    private RebDataRow createRebDataRow(String clsId, String clsFullnm, String dtaVal, String wrttimeDesc) {
        RebDataRow rebDataRow = new RebDataRow();
        try {
            setField(rebDataRow, "clsId", clsId);
            setField(rebDataRow, "clsFullnm", clsFullnm);
            setField(rebDataRow, "dtaVal", dtaVal != null ? new BigDecimal(dtaVal) : null);
            setField(rebDataRow, "wrttimeDesc", wrttimeDesc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return rebDataRow;
    }
    
    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
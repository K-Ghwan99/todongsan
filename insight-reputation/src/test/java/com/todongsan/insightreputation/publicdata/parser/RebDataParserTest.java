package com.todongsan.insightreputation.publicdata.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todongsan.insightreputation.enums.PublicDataSource;
import com.todongsan.insightreputation.enums.PublicDataType;
import com.todongsan.insightreputation.publicdata.dto.RebDataRow;
import com.todongsan.insightreputation.publicdata.entity.PublicDataSnapshot;
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
    @DisplayName("REB 데이터 파싱 - 주간 정상 데이터 처리 성공 (ERD 컬럼 전체 매핑)")
    void parseRebData_weeklyValidData_success() throws Exception {
        // given
        RebDataRow rebData = createRebDataRow(50071L, "경기>경부1권>과천시", 10001L, "지수", 100.5, "2012-05-07", "WK");
        List<RebDataRow> rebDataRows = Arrays.asList(rebData);
        
        // when
        List<PublicDataSnapshot> result = parser.parseRebData(rebDataRows);
        
        // then
        assertEquals(1, result.size());
        
        PublicDataSnapshot snapshot = result.get(0);
        // ERD 컬럼 전체 검증
        assertEquals(PublicDataSource.REB, snapshot.getSource());
        assertEquals(PublicDataType.WEEKLY_PRICE_INDEX, snapshot.getDataType());
        assertEquals(LocalDate.of(2012, 5, 7), snapshot.getReferenceDate());
        assertEquals("경기", snapshot.getRegionSido());
        assertEquals("50071", snapshot.getSourceRegionId());
        assertEquals("경기>경부1권>과천시", snapshot.getRegionFullpath());
        assertEquals("10001", snapshot.getItmId());
        assertEquals("지수", snapshot.getItmNm());
        assertEquals(new BigDecimal("100.5"), snapshot.getNumericValue());
        assertNotNull(snapshot.getRawData());
        assertNotNull(snapshot.getCollectedAt()); // collected_at 세팅 확인
    }
    
    @Test
    @DisplayName("REB 데이터 파싱 - 월간 정상 데이터 처리 성공 (해당 월 1일)")
    void parseRebData_monthlyValidData_success() throws Exception {
        // given
        RebDataRow rebData = createRebDataRow(510085L, "전북>전주시", 100001L, "변동률", 51.5, "2003년 11월", "MM");
        List<RebDataRow> rebDataRows = Arrays.asList(rebData);
        
        // when
        List<PublicDataSnapshot> result = parser.parseRebData(rebDataRows);
        
        // then
        assertEquals(1, result.size());
        
        PublicDataSnapshot snapshot = result.get(0);
        assertEquals(PublicDataSource.REB, snapshot.getSource());
        assertEquals(PublicDataType.MONTHLY_PRICE_INDEX, snapshot.getDataType());
        assertEquals(LocalDate.of(2003, 11, 1), snapshot.getReferenceDate()); // 월간은 해당 월 1일
        assertEquals("전북", snapshot.getRegionSido());
        assertEquals("510085", snapshot.getSourceRegionId());
        assertEquals("전북>전주시", snapshot.getRegionFullpath());
        assertEquals(new BigDecimal("51.5"), snapshot.getNumericValue());
        assertNotNull(snapshot.getRawData());
        assertNotNull(snapshot.getCollectedAt()); // collected_at 세팅 확인
    }
    
    @Test
    @DisplayName("REB 데이터 파싱 - 전국/수도권/지방 등 '>' 없는 지역 처리 (각각 고유한 source_region_id)")
    void parseRebData_nationalLevelData_handledCorrectly() throws Exception {
        // given
        RebDataRow rebData1 = createRebDataRow(50001L, "부산>해운대구>우동", 10001L, "지수", 98.7, "2025-05-15", "WK");
        RebDataRow rebData2 = createRebDataRow(50002L, "대구>중구", 10001L, "지수", 102.1, "2025-05-15", "WK");
        RebDataRow rebData3 = createRebDataRow(10001L, "전국", 10001L, "지수", 100.0, "2025-05-15", "WK"); // 전국
        RebDataRow rebData4 = createRebDataRow(20001L, "수도권", 10001L, "지수", 105.0, "2025-05-15", "WK"); // 수도권
        RebDataRow rebData5 = createRebDataRow(30001L, "지방", 10001L, "지수", 95.0, "2025-05-15", "WK"); // 지방
        List<RebDataRow> rebDataRows = Arrays.asList(rebData1, rebData2, rebData3, rebData4, rebData5);
        
        // when
        List<PublicDataSnapshot> result = parser.parseRebData(rebDataRows);
        
        // then
        assertEquals(5, result.size());
        
        assertEquals("부산", result.get(0).getRegionSido());
        assertEquals("50001", result.get(0).getSourceRegionId());
        
        assertEquals("대구", result.get(1).getRegionSido());
        assertEquals("50002", result.get(1).getSourceRegionId());
        
        // '>' 없는 지역들은 clsFullnm 그대로 region_sido 사용, sourceRegionId는 각각 고유
        assertEquals("전국", result.get(2).getRegionSido());
        assertEquals("10001", result.get(2).getSourceRegionId()); // CLS_ID 그대로 사용
        
        assertEquals("수도권", result.get(3).getRegionSido());
        assertEquals("20001", result.get(3).getSourceRegionId()); // CLS_ID 그대로 사용
        
        assertEquals("지방", result.get(4).getRegionSido());
        assertEquals("30001", result.get(4).getSourceRegionId()); // CLS_ID 그대로 사용
    }
    
    @Test
    @DisplayName("REB 데이터 파싱 - 필수 필드 누락 시 해당 행 건너뜀")
    void parseRebData_missingRequiredFields_skipsRow() throws Exception {
        // given
        RebDataRow validData = createRebDataRow(1001L, "서울>강남구", 10001L, "지수", 100.5, "2025-06-01", "WK");
        RebDataRow invalidData1 = createRebDataRow(null, "서울>강남구", 10001L, "지수", 100.5, "2025-06-01", "WK"); // clsId null
        RebDataRow invalidData2 = createRebDataRow(1002L, null, 10001L, "지수", 100.5, "2025-06-01", "WK"); // clsFullnm null
        RebDataRow invalidData3 = createRebDataRow(1003L, "서울>강남구", null, "지수", 100.5, null, "WK"); // itmId null, wrtimeDesc null
        
        List<RebDataRow> rebDataRows = Arrays.asList(validData, invalidData1, invalidData2, invalidData3);
        
        // when
        List<PublicDataSnapshot> result = parser.parseRebData(rebDataRows);
        
        // then
        assertEquals(1, result.size()); // 유효한 데이터 1개만 처리
        assertEquals("1001", result.get(0).getSourceRegionId());
        assertEquals(PublicDataSource.REB, result.get(0).getSource());
        assertEquals(PublicDataType.WEEKLY_PRICE_INDEX, result.get(0).getDataType());
    }

    @Test
    @DisplayName("REB 데이터 파싱 - 잘못된 날짜 형식 시 해당 행 건너뜀")
    void parseRebData_invalidDateFormat_skipsRow() throws Exception {
        // given
        RebDataRow validData = createRebDataRow(1001L, "서울>강남구", 10001L, "지수", 100.5, "2025-06-01", "WK");
        RebDataRow invalidDateData = createRebDataRow(1002L, "서울>서초구", 10001L, "지수", 99.8, "invalid-date", "WK");
        
        List<RebDataRow> rebDataRows = Arrays.asList(validData, invalidDateData);
        
        // when
        List<PublicDataSnapshot> result = parser.parseRebData(rebDataRows);
        
        // then
        assertEquals(1, result.size()); // 유효한 데이터 1개만 처리
        assertEquals("1001", result.get(0).getSourceRegionId());
        assertEquals(LocalDate.of(2025, 6, 1), result.get(0).getReferenceDate());
        assertEquals("서울", result.get(0).getRegionSido());
    }
    
    @Test
    @DisplayName("REB 데이터 파싱 - 빈 리스트 입력 시 빈 결과 반환")
    void parseRebData_emptyInput_returnsEmptyList() throws Exception {
        // given
        List<RebDataRow> emptyList = Collections.emptyList();
        
        // when
        List<PublicDataSnapshot> result = parser.parseRebData(emptyList);
        
        // then
        assertTrue(result.isEmpty());
    }
    
    @Test
    @DisplayName("REB 데이터 파싱 - 수치값 null 허용")
    void parseRebData_nullNumericValue_allowsNull() throws Exception {
        // given
        RebDataRow rebData = createRebDataRow(1001L, "서울>강남구", 10001L, "지수", null, "2025-06-01", "WK");
        List<RebDataRow> rebDataRows = Arrays.asList(rebData);
        
        // when
        List<PublicDataSnapshot> result = parser.parseRebData(rebDataRows);
        
        // then
        assertEquals(1, result.size());
        assertNull(result.get(0).getNumericValue());
        assertEquals(PublicDataSource.REB, result.get(0).getSource());
        assertEquals(PublicDataType.WEEKLY_PRICE_INDEX, result.get(0).getDataType());
        assertEquals("1001", result.get(0).getSourceRegionId());
    }
    
    @Test
    @DisplayName("REB 데이터 파싱 - 공백 문자 처리")
    void parseRebData_whitespaceHandling_trimsCorrectly() throws Exception {
        // given
        RebDataRow rebData = createRebDataRow(1001L, "  서울>강남구  ", 10001L, "지수", 100.5, "  2025-06-01  ", "WK");
        List<RebDataRow> rebDataRows = Arrays.asList(rebData);
        
        // when
        List<PublicDataSnapshot> result = parser.parseRebData(rebDataRows);
        
        // then
        assertEquals(1, result.size());
        assertEquals("서울", result.get(0).getRegionSido());
        assertEquals(LocalDate.of(2025, 6, 1), result.get(0).getReferenceDate());
        assertEquals("  서울>강남구  ", result.get(0).getRegionFullpath()); // regionFullpath는 trim하지 않음
        assertNotNull(result.get(0).getCollectedAt());
    }
    
    private RebDataRow createRebDataRow(Long clsId, String clsFullnm, Long itmId, String itmNm, Double dtaVal, String wrtimeDesc, String dtaCycleCd) {
        RebDataRow rebDataRow = new RebDataRow();
        try {
            setField(rebDataRow, "clsId", clsId);
            setField(rebDataRow, "clsFullnm", clsFullnm);
            setField(rebDataRow, "itmId", itmId);
            setField(rebDataRow, "itmNm", itmNm);
            setField(rebDataRow, "dtaVal", dtaVal);
            setField(rebDataRow, "wrtimeDesc", wrtimeDesc);
            setField(rebDataRow, "dtaCycleCd", dtaCycleCd);
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
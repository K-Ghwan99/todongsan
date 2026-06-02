package com.todongsan.insightreputation.publicdata.service;

import com.todongsan.insightreputation.enums.PublicDataSource;
import com.todongsan.insightreputation.enums.PublicDataType;
import com.todongsan.insightreputation.publicdata.client.RebApiClient;
import com.todongsan.insightreputation.publicdata.dto.ParsedDataRow;
import com.todongsan.insightreputation.publicdata.dto.RebDataRow;
import com.todongsan.insightreputation.publicdata.parser.RebDataParser;
import com.todongsan.insightreputation.publicdata.repository.PublicDataSnapshotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicDataBatchServiceTest {
    
    @InjectMocks
    private PublicDataBatchService batchService;
    
    @Mock
    private RebApiClient rebApiClient;
    
    @Mock
    private RebDataParser rebDataParser;
    
    @Mock
    private PublicDataSnapshotRepository repository;
    
    @Test
    @DisplayName("주간 가격지수 수집 - 정상 데이터 처리 성공")
    void collectWeeklyPriceIndex_validData_success() throws Exception {
        // given
        RebDataRow mockRebData = createMockRebDataRow("1001", "서울>강남구", "100.5", "2025.06.01");
        List<RebDataRow> rebDataRows = Arrays.asList(mockRebData);
        
        ParsedDataRow mockParsedData = createMockParsedDataRow(
            PublicDataType.WEEKLY_PRICE_INDEX, 
            LocalDate.of(2025, 6, 1), 
            "서울", 
            "1001", 
            new BigDecimal("100.5")
        );
        List<ParsedDataRow> parsedData = Arrays.asList(mockParsedData);
        
        when(rebApiClient.fetchWeeklyPriceIndex()).thenReturn(rebDataRows);
        when(rebDataParser.parseRebData(rebDataRows, PublicDataType.WEEKLY_PRICE_INDEX))
            .thenReturn(parsedData);
        
        // when
        batchService.collectWeeklyPriceIndex();
        
        // then
        verify(rebApiClient).fetchWeeklyPriceIndex();
        verify(rebDataParser).parseRebData(rebDataRows, PublicDataType.WEEKLY_PRICE_INDEX);
        verify(repository).upsertSnapshot(
            eq("REB"),
            eq("WEEKLY_PRICE_INDEX"),
            eq(LocalDate.of(2025, 6, 1)),
            eq("서울"),
            eq("1001"),
            anyString(),
            eq(new BigDecimal("100.5")),
            anyString(),
            any(),
            any(),
            any()
        );
    }
    
    @Test
    @DisplayName("주간 가격지수 수집 - API 응답 데이터 없음")
    void collectWeeklyPriceIndex_emptyApiResponse_noProcessing() throws Exception {
        // given
        when(rebApiClient.fetchWeeklyPriceIndex()).thenReturn(Collections.emptyList());
        
        // when
        batchService.collectWeeklyPriceIndex();
        
        // then
        verify(rebApiClient).fetchWeeklyPriceIndex();
        verify(rebDataParser, never()).parseRebData(any(), any());
        verify(repository, never()).upsertSnapshot(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
    
    @Test
    @DisplayName("주간 가격지수 수집 - 파싱 결과 없음")
    void collectWeeklyPriceIndex_emptyParsedData_noStorage() throws Exception {
        // given
        RebDataRow mockRebData = createMockRebDataRow("1001", "서울>강남구", "100.5", "2025.06.01");
        List<RebDataRow> rebDataRows = Arrays.asList(mockRebData);
        
        when(rebApiClient.fetchWeeklyPriceIndex()).thenReturn(rebDataRows);
        when(rebDataParser.parseRebData(rebDataRows, PublicDataType.WEEKLY_PRICE_INDEX))
            .thenReturn(Collections.emptyList());
        
        // when
        batchService.collectWeeklyPriceIndex();
        
        // then
        verify(rebApiClient).fetchWeeklyPriceIndex();
        verify(rebDataParser).parseRebData(rebDataRows, PublicDataType.WEEKLY_PRICE_INDEX);
        verify(repository, never()).upsertSnapshot(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
    
    @Test
    @DisplayName("월간 가격지수 수집 - 정상 데이터 처리 성공")
    void collectMonthlyPriceIndex_validData_success() throws Exception {
        // given
        RebDataRow mockRebData = createMockRebDataRow("2001", "부산>해운대구", "98.7", "2025.05.15");
        List<RebDataRow> rebDataRows = Arrays.asList(mockRebData);
        
        ParsedDataRow mockParsedData = createMockParsedDataRow(
            PublicDataType.MONTHLY_PRICE_INDEX, 
            LocalDate.of(2025, 5, 15), 
            "부산", 
            "2001", 
            new BigDecimal("98.7")
        );
        List<ParsedDataRow> parsedData = Arrays.asList(mockParsedData);
        
        when(rebApiClient.fetchMonthlyPriceIndex()).thenReturn(rebDataRows);
        when(rebDataParser.parseRebData(rebDataRows, PublicDataType.MONTHLY_PRICE_INDEX))
            .thenReturn(parsedData);
        
        // when
        batchService.collectMonthlyPriceIndex();
        
        // then
        verify(rebApiClient).fetchMonthlyPriceIndex();
        verify(rebDataParser).parseRebData(rebDataRows, PublicDataType.MONTHLY_PRICE_INDEX);
        verify(repository).upsertSnapshot(
            eq("REB"),
            eq("MONTHLY_PRICE_INDEX"),
            eq(LocalDate.of(2025, 5, 15)),
            eq("부산"),
            eq("2001"),
            anyString(),
            eq(new BigDecimal("98.7")),
            anyString(),
            any(),
            any(),
            any()
        );
    }
    
    @Test
    @DisplayName("월간 가격지수 수집 - 저장 중 일부 실패해도 전체 배치는 계속 진행")
    void collectMonthlyPriceIndex_partialFailure_continuesProcessing() throws Exception {
        // given
        RebDataRow mockRebData1 = createMockRebDataRow("3001", "대구>중구", "102.1", "2025.05.15");
        RebDataRow mockRebData2 = createMockRebDataRow("3002", "대구>동구", "101.8", "2025.05.15");
        List<RebDataRow> rebDataRows = Arrays.asList(mockRebData1, mockRebData2);
        
        ParsedDataRow mockParsedData1 = createMockParsedDataRow(
            PublicDataType.MONTHLY_PRICE_INDEX, LocalDate.of(2025, 5, 15), "대구", "3001", new BigDecimal("102.1"));
        ParsedDataRow mockParsedData2 = createMockParsedDataRow(
            PublicDataType.MONTHLY_PRICE_INDEX, LocalDate.of(2025, 5, 15), "대구", "3002", new BigDecimal("101.8"));
        List<ParsedDataRow> parsedData = Arrays.asList(mockParsedData1, mockParsedData2);
        
        when(rebApiClient.fetchMonthlyPriceIndex()).thenReturn(rebDataRows);
        when(rebDataParser.parseRebData(rebDataRows, PublicDataType.MONTHLY_PRICE_INDEX))
            .thenReturn(parsedData);
        
        // 첫 번째 데이터 저장 실패, 두 번째는 성공
        doThrow(new RuntimeException("DB 저장 오류"))
            .doNothing()
            .when(repository).upsertSnapshot(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        
        // when
        batchService.collectMonthlyPriceIndex();
        
        // then
        verify(rebApiClient).fetchMonthlyPriceIndex();
        verify(rebDataParser).parseRebData(rebDataRows, PublicDataType.MONTHLY_PRICE_INDEX);
        verify(repository, times(2)).upsertSnapshot(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
    
    private RebDataRow createMockRebDataRow(String clsId, String clsFullnm, String dtaVal, String wrttimeDesc) {
        RebDataRow rebDataRow = new RebDataRow();
        // 리플렉션으로 필드 설정 (실제로는 생성자나 빌더 패턴 사용 권장)
        try {
            setField(rebDataRow, "clsId", clsId);
            setField(rebDataRow, "clsFullnm", clsFullnm);
            setField(rebDataRow, "dtaVal", new BigDecimal(dtaVal));
            setField(rebDataRow, "wrttimeDesc", wrttimeDesc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return rebDataRow;
    }
    
    private ParsedDataRow createMockParsedDataRow(PublicDataType dataType, LocalDate referenceDate, 
                                                  String regionSido, String sourceRegionId, BigDecimal numericValue) {
        return ParsedDataRow.builder()
            .source(PublicDataSource.REB)
            .dataType(dataType)
            .referenceDate(referenceDate)
            .regionSido(regionSido)
            .sourceRegionId(sourceRegionId)
            .regionFullpath(regionSido + ">" + sourceRegionId)
            .numericValue(numericValue)
            .rawData("{\"test\": \"data\"}")
            .build();
    }
    
    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
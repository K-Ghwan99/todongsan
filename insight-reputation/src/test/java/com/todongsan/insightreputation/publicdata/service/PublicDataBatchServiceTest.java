package com.todongsan.insightreputation.publicdata.service;

import com.todongsan.insightreputation.enums.PublicDataSource;
import com.todongsan.insightreputation.enums.PublicDataType;
import com.todongsan.insightreputation.publicdata.client.RebApiClient;
import com.todongsan.insightreputation.publicdata.dto.RebDataRow;
import com.todongsan.insightreputation.publicdata.entity.PublicDataSnapshot;
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
        RebDataRow mockRebData = createMockRebDataRow(1001L, "서울>강남구", 100.5, "2025-06-01", "WK");
        List<RebDataRow> rebDataRows = Arrays.asList(mockRebData);
        
        PublicDataSnapshot mockSnapshot = createMockPublicDataSnapshot(
            PublicDataSource.REB,
            PublicDataType.PRICE_INDEX, 
            LocalDate.of(2025, 6, 1), 
            "서울", 
            "1001", 
            "서울>강남구",
            new BigDecimal("100.5")
        );
        List<PublicDataSnapshot> parsedData = Arrays.asList(mockSnapshot);
        
        when(rebApiClient.fetchWeeklyPriceIndex()).thenReturn(rebDataRows);
        when(rebDataParser.parseRebData(rebDataRows)).thenReturn(parsedData);
        
        // when
        batchService.collectWeeklyPriceIndex();
        
        // then
        verify(rebApiClient).fetchWeeklyPriceIndex();
        verify(rebDataParser).parseRebData(rebDataRows);
        verify(repository).save(any(PublicDataSnapshot.class));
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
        verify(rebDataParser, never()).parseRebData(any());
        verify(repository, never()).save(any());
    }
    
    @Test
    @DisplayName("주간 가격지수 수집 - 파싱 결과 없음")
    void collectWeeklyPriceIndex_emptyParsedData_noStorage() throws Exception {
        // given
        RebDataRow mockRebData = createMockRebDataRow(1001L, "서울>강남구", 100.5, "2025-06-01", "WK");
        List<RebDataRow> rebDataRows = Arrays.asList(mockRebData);
        
        when(rebApiClient.fetchWeeklyPriceIndex()).thenReturn(rebDataRows);
        when(rebDataParser.parseRebData(rebDataRows)).thenReturn(Collections.emptyList());
        
        // when
        batchService.collectWeeklyPriceIndex();
        
        // then
        verify(rebApiClient).fetchWeeklyPriceIndex();
        verify(rebDataParser).parseRebData(rebDataRows);
        verify(repository, never()).save(any());
    }
    
    @Test
    @DisplayName("월간 가격지수 수집 - 정상 데이터 처리 성공")
    void collectMonthlyPriceIndex_validData_success() throws Exception {
        // given
        RebDataRow mockRebData = createMockRebDataRow(2001L, "부산>해운대구", 98.7, "2025년 05월", "MM");
        List<RebDataRow> rebDataRows = Arrays.asList(mockRebData);
        
        PublicDataSnapshot mockSnapshot = createMockPublicDataSnapshot(
            PublicDataSource.REB,
            PublicDataType.PRICE_INDEX, 
            LocalDate.of(2025, 5, 1), // 월간은 해당 월 1일
            "부산", 
            "2001", 
            "부산>해운대구",
            new BigDecimal("98.7")
        );
        List<PublicDataSnapshot> parsedData = Arrays.asList(mockSnapshot);
        
        when(rebApiClient.fetchMonthlyPriceIndex()).thenReturn(rebDataRows);
        when(rebDataParser.parseRebData(rebDataRows)).thenReturn(parsedData);
        
        // when
        batchService.collectMonthlyPriceIndex();
        
        // then
        verify(rebApiClient).fetchMonthlyPriceIndex();
        verify(rebDataParser).parseRebData(rebDataRows);
        verify(repository).save(any(PublicDataSnapshot.class));
    }
    
    @Test
    @DisplayName("월간 가격지수 수집 - 저장 중 일부 실패해도 전체 배치는 계속 진행")
    void collectMonthlyPriceIndex_partialFailure_continuesProcessing() throws Exception {
        // given
        RebDataRow mockRebData1 = createMockRebDataRow(3001L, "대구>중구", 102.1, "2025년 05월", "MM");
        RebDataRow mockRebData2 = createMockRebDataRow(3002L, "대구>동구", 101.8, "2025년 05월", "MM");
        List<RebDataRow> rebDataRows = Arrays.asList(mockRebData1, mockRebData2);
        
        PublicDataSnapshot mockSnapshot1 = createMockPublicDataSnapshot(
            PublicDataSource.REB, PublicDataType.PRICE_INDEX, LocalDate.of(2025, 5, 1), "대구", "3001", "대구>중구", new BigDecimal("102.1"));
        PublicDataSnapshot mockSnapshot2 = createMockPublicDataSnapshot(
            PublicDataSource.REB, PublicDataType.PRICE_INDEX, LocalDate.of(2025, 5, 1), "대구", "3002", "대구>동구", new BigDecimal("101.8"));
        List<PublicDataSnapshot> parsedData = Arrays.asList(mockSnapshot1, mockSnapshot2);
        
        when(rebApiClient.fetchMonthlyPriceIndex()).thenReturn(rebDataRows);
        when(rebDataParser.parseRebData(rebDataRows)).thenReturn(parsedData);
        
        // 첫 번째 데이터 저장 실패, 두 번째는 성공
        when(repository.save(any(PublicDataSnapshot.class)))
            .thenThrow(new RuntimeException("DB 저장 오류"))
            .thenReturn(mockSnapshot2); // 두 번째 호출에서는 성공
        
        // when
        batchService.collectMonthlyPriceIndex();
        
        // then
        verify(rebApiClient).fetchMonthlyPriceIndex();
        verify(rebDataParser).parseRebData(rebDataRows);
        verify(repository, times(2)).save(any(PublicDataSnapshot.class));
    }
    
    private RebDataRow createMockRebDataRow(Long clsId, String clsFullnm, Double dtaVal, String wrtimeDesc, String dtaCycleCd) {
        RebDataRow rebDataRow = new RebDataRow();
        // 리플렉션으로 필드 설정 (실제로는 생성자나 빌더 패턴 사용 권장)
        try {
            setField(rebDataRow, "clsId", clsId);
            setField(rebDataRow, "clsFullnm", clsFullnm);
            setField(rebDataRow, "dtaVal", dtaVal);
            setField(rebDataRow, "wrtimeDesc", wrtimeDesc);
            setField(rebDataRow, "dtaCycleCd", dtaCycleCd);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return rebDataRow;
    }
    
    private PublicDataSnapshot createMockPublicDataSnapshot(PublicDataSource source, PublicDataType dataType, LocalDate referenceDate, 
                                                           String regionSido, String sourceRegionId, String regionFullpath, BigDecimal numericValue) {
        return PublicDataSnapshot.builder()
            .source(source)
            .dataType(dataType)
            .referenceDate(referenceDate)
            .regionSido(regionSido)
            .sourceRegionId(sourceRegionId)
            .regionFullpath(regionFullpath)
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
package com.todongsan.insightreputation.publicdata.service;

import com.todongsan.insightreputation.enums.PublicDataType;
import com.todongsan.insightreputation.publicdata.client.RebApiClient;
import com.todongsan.insightreputation.publicdata.dto.ParsedDataRow;
import com.todongsan.insightreputation.publicdata.dto.RebDataRow;
import com.todongsan.insightreputation.publicdata.parser.RebDataParser;
import com.todongsan.insightreputation.publicdata.repository.PublicDataSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicDataBatchService {
    
    private final RebApiClient rebApiClient;
    private final RebDataParser rebDataParser;
    private final PublicDataSnapshotRepository repository;
    
    /**
     * REB 주간 매매가격지수 배치 수집
     */
    @Transactional
    public void collectWeeklyPriceIndex() {
        log.info("REB 주간 매매가격지수 배치 수집 시작");
        
        try {
            // 1. R-ONE API에서 주간 데이터 조회
            List<RebDataRow> rebDataRows = rebApiClient.fetchWeeklyPriceIndex();
            
            if (rebDataRows.isEmpty()) {
                log.warn("REB 주간 데이터 조회 결과 없음");
                return;
            }
            
            // 2. 데이터 파싱 및 변환
            List<ParsedDataRow> parsedData = rebDataParser.parseRebData(rebDataRows, PublicDataType.WEEKLY_PRICE_INDEX);
            
            if (parsedData.isEmpty()) {
                log.warn("REB 주간 데이터 파싱 결과 없음");
                return;
            }
            
            // 3. 데이터베이스 저장 (ON DUPLICATE KEY UPDATE)
            LocalDateTime collectedAt = LocalDateTime.now();
            int savedCount = 0;
            
            for (ParsedDataRow data : parsedData) {
                try {
                    repository.upsertSnapshot(
                        data.getSource().name(),
                        data.getDataType().name(),
                        data.getReferenceDate(),
                        data.getRegionSido(),
                        data.getSourceRegionId(),
                        data.getRegionFullpath(),
                        data.getNumericValue(),
                        data.getRawData(),
                        collectedAt,
                        collectedAt, // created_at
                        collectedAt  // updated_at
                    );
                    savedCount++;
                } catch (Exception e) {
                    log.error("REB 주간 데이터 저장 실패, 건너뜀: sourceRegionId={}, error={}", 
                             data.getSourceRegionId(), e.getMessage());
                }
            }
            
            log.info("REB 주간 매매가격지수 배치 수집 완료: 조회={}, 파싱={}, 저장={}", 
                    rebDataRows.size(), parsedData.size(), savedCount);
                    
        } catch (Exception e) {
            log.error("REB 주간 매매가격지수 배치 수집 실패", e);
            throw e;
        }
    }
    
    /**
     * REB 월간 매매가격지수 배치 수집
     */
    @Transactional
    public void collectMonthlyPriceIndex() {
        log.info("REB 월간 매매가격지수 배치 수집 시작");
        
        try {
            // 1. R-ONE API에서 월간 데이터 조회
            List<RebDataRow> rebDataRows = rebApiClient.fetchMonthlyPriceIndex();
            
            if (rebDataRows.isEmpty()) {
                log.warn("REB 월간 데이터 조회 결과 없음");
                return;
            }
            
            // 2. 데이터 파싱 및 변환
            List<ParsedDataRow> parsedData = rebDataParser.parseRebData(rebDataRows, PublicDataType.MONTHLY_PRICE_INDEX);
            
            if (parsedData.isEmpty()) {
                log.warn("REB 월간 데이터 파싱 결과 없음");
                return;
            }
            
            // 3. 데이터베이스 저장 (ON DUPLICATE KEY UPDATE)
            LocalDateTime collectedAt = LocalDateTime.now();
            int savedCount = 0;
            
            for (ParsedDataRow data : parsedData) {
                try {
                    repository.upsertSnapshot(
                        data.getSource().name(),
                        data.getDataType().name(),
                        data.getReferenceDate(),
                        data.getRegionSido(),
                        data.getSourceRegionId(),
                        data.getRegionFullpath(),
                        data.getNumericValue(),
                        data.getRawData(),
                        collectedAt,
                        collectedAt, // created_at
                        collectedAt  // updated_at
                    );
                    savedCount++;
                } catch (Exception e) {
                    log.error("REB 월간 데이터 저장 실패, 건너뜀: sourceRegionId={}, error={}", 
                             data.getSourceRegionId(), e.getMessage());
                }
            }
            
            log.info("REB 월간 매매가격지수 배치 수집 완료: 조회={}, 파싱={}, 저장={}", 
                    rebDataRows.size(), parsedData.size(), savedCount);
                    
        } catch (Exception e) {
            log.error("REB 월간 매매가격지수 배치 수집 실패", e);
            throw e;
        }
    }
}
package com.todongsan.insightreputation.publicdata.service;

import com.todongsan.insightreputation.publicdata.client.RebApiClient;
import com.todongsan.insightreputation.publicdata.dto.RebDataRow;
import com.todongsan.insightreputation.publicdata.entity.PublicDataSnapshot;
import com.todongsan.insightreputation.publicdata.parser.RebDataParser;
import com.todongsan.insightreputation.publicdata.repository.PublicDataSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
            List<PublicDataSnapshot> parsedData = rebDataParser.parseRebData(rebDataRows);
            
            if (parsedData.isEmpty()) {
                log.warn("REB 주간 데이터 파싱 결과 없음");
                return;
            }
            
            // 3. 데이터베이스 저장 (saveAll로 간편화, JPA가 내부적으로 upsert 처리)
            int savedCount = 0;
            
            for (PublicDataSnapshot snapshot : parsedData) {
                try {
                    repository.save(snapshot);
                    savedCount++;
                } catch (Exception e) {
                    log.error("REB 주간 데이터 저장 실패, 건너뜀: sourceRegionId={}, error={}", 
                             snapshot.getSourceRegionId(), e.getMessage());
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
            List<PublicDataSnapshot> parsedData = rebDataParser.parseRebData(rebDataRows);
            
            if (parsedData.isEmpty()) {
                log.warn("REB 월간 데이터 파싱 결과 없음");
                return;
            }
            
            // 3. 데이터베이스 저장
            int savedCount = 0;
            
            for (PublicDataSnapshot snapshot : parsedData) {
                try {
                    repository.save(snapshot);
                    savedCount++;
                } catch (Exception e) {
                    log.error("REB 월간 데이터 저장 실패, 건너뜀: sourceRegionId={}, error={}", 
                             snapshot.getSourceRegionId(), e.getMessage());
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
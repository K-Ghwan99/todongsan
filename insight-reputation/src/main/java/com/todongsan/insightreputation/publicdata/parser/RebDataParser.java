package com.todongsan.insightreputation.publicdata.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todongsan.insightreputation.enums.PublicDataSource;
import com.todongsan.insightreputation.enums.PublicDataType;
import com.todongsan.insightreputation.publicdata.dto.RebDataRow;
import com.todongsan.insightreputation.publicdata.entity.PublicDataSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RebDataParser {

    private final ObjectMapper objectMapper;
    
    // 주간 날짜 형식: "2012-05-07"
    private static final DateTimeFormatter WEEKLY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // 월간 날짜 형식: "2003년 11월"
    private static final DateTimeFormatter MONTHLY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월");

    /**
     * REB 원본 데이터를 PublicDataSnapshot으로 변환
     * 
     * @param rebDataRows REB API 응답 데이터
     * @return 파싱된 데이터 목록
     */
    public List<PublicDataSnapshot> parseRebData(List<RebDataRow> rebDataRows) {
        List<PublicDataSnapshot> parsedData = new ArrayList<>();
        
        log.info("REB 데이터 파싱 시작: inputSize={}", rebDataRows.size());
        
        for (RebDataRow row : rebDataRows) {
            try {
                PublicDataSnapshot snapshot = parseRebDataRow(row);
                if (snapshot != null) {
                    parsedData.add(snapshot);
                }
            } catch (Exception e) {
                log.warn("REB 데이터 행 파싱 실패, 건너뜀: clsId={}, clsFullnm={}, error={}", 
                        row.getClsId(), row.getClsFullnm(), e.getMessage());
            }
        }
        
        log.info("REB 데이터 파싱 완료: outputSize={}", parsedData.size());
        return parsedData;
    }

    /**
     * REB 데이터 단일 행을 PublicDataSnapshot으로 파싱 (ERD 컬럼 전체 매핑)
     */
    private PublicDataSnapshot parseRebDataRow(RebDataRow row) {
        // 필수 필드 검증
        if (row.getClsId() == null || row.getClsFullnm() == null || row.getWrtimeDesc() == null || row.getItmId() == null) {
            log.debug("필수 필드 누락으로 행 건너뜀: clsId={}, clsFullnm={}, wrtimeDesc={}, itmId={}", 
                     row.getClsId(), row.getClsFullnm(), row.getWrtimeDesc(), row.getItmId());
            return null;
        }

        try {
            // 1. source_region_id: clsId를 String으로 변환
            String sourceRegionId = String.valueOf(row.getClsId());
            
            // 2. region_fullpath: cls_fullnm 그대로 사용
            String regionFullpath = row.getClsFullnm();
            
            // 3. region_sido 파싱: cls_fullnm의 첫 번째 '>' 이전 값
            String regionSido = parseRegionSido(row.getClsFullnm());
            
            // 전국 단위 처리: '>' 없으면 전국으로 간주하고 sourceRegionId를 "50001"로 설정
            if (!row.getClsFullnm().contains(">")) {
                regionSido = "전국";
                sourceRegionId = "50001";
            }
            
            // 4. itm_id: itmId를 String으로 변환
            String itmId = String.valueOf(row.getItmId());
            
            // 5. itm_nm: itmNm 그대로 사용
            String itmNm = row.getItmNm();
            
            // 6. reference_date: wrtimeDesc 파싱 (주간/월간 분기)
            LocalDate referenceDate = parseReferenceDate(row.getWrtimeDesc(), row.getDtaCycleCd());
            
            // 7. numeric_value: dtaVal을 BigDecimal로 변환 (null 허용)
            BigDecimal numericValue = row.getDtaVal() != null ? BigDecimal.valueOf(row.getDtaVal()) : null;
            
            // 8. raw_data: 원본 JSON 직렬화
            String rawData = objectMapper.writeValueAsString(row);

            // ERD 컬럼 전체 매핑하여 PublicDataSnapshot 생성
            return PublicDataSnapshot.builder()
                    .source(PublicDataSource.REB)                    // source: 'REB' 고정
                    .dataType(PublicDataType.PRICE_INDEX)           // data_type: 'PRICE_INDEX' 고정
                    .referenceDate(referenceDate)                   // reference_date
                    .regionSido(regionSido)                         // region_sido
                    .sourceRegionId(sourceRegionId)                 // source_region_id
                    .regionFullpath(regionFullpath)                 // region_fullpath
                    .itmId(itmId)                                   // itm_id
                    .itmNm(itmNm)                                   // itm_nm
                    .numericValue(numericValue)                     // numeric_value
                    .rawData(rawData)                               // raw_data (JSON)
                    // collected_at, created_at은 Builder에서 자동 설정
                    .build();
            
        } catch (JsonProcessingException e) {
            log.error("REB 데이터 JSON 직렬화 실패: clsId={}", row.getClsId(), e);
            return null;
        } catch (Exception e) {
            log.error("REB 데이터 행 파싱 중 오류: clsId={}", row.getClsId(), e);
            return null;
        }
    }

    /**
     * cls_fullnm에서 region_sido 추출
     * 예: "서울>강북지역>도심권>종로구" → "서울"
     * 예: "전국" → "전국"
     */
    private String parseRegionSido(String clsFullnm) {
        if (clsFullnm == null || clsFullnm.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = clsFullnm.trim();
        
        // '>' 문자가 없으면 전체가 sido (예: "전국")
        int separatorIndex = trimmed.indexOf('>');
        if (separatorIndex == -1) {
            return trimmed;
        }
        
        // 첫 번째 '>' 이전 부분 반환
        return trimmed.substring(0, separatorIndex);
    }

    /**
     * wrtimeDesc를 LocalDate로 변환 (주간/월간 분기 처리)
     * 주간 (DTACYCLE_CD = "WK"): "2012-05-07" → 2012-05-07
     * 월간 (DTACYCLE_CD = "MM"): "2003년 11월" → 2003-11-01 (해당 월 1일)
     */
    private LocalDate parseReferenceDate(String wrtimeDesc, String dtaCycleCd) {
        if (wrtimeDesc == null || wrtimeDesc.trim().isEmpty()) {
            throw new IllegalArgumentException("wrtimeDesc가 비어있음");
        }
        
        String trimmed = wrtimeDesc.trim();
        
        try {
            if ("WK".equals(dtaCycleCd)) {
                // 주간: "2012-05-07" 형식
                return LocalDate.parse(trimmed, WEEKLY_DATE_FORMATTER);
            } else if ("MM".equals(dtaCycleCd)) {
                // 월간: "2003년 11월" 형식 → 해당 월 1일로 변환
                java.time.YearMonth yearMonth = java.time.YearMonth.parse(trimmed, MONTHLY_DATE_FORMATTER);
                return yearMonth.atDay(1);
            } else {
                // 기본값으로 주간 형식 시도
                return LocalDate.parse(trimmed, WEEKLY_DATE_FORMATTER);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("wrtimeDesc 파싱 실패: " + wrtimeDesc + ", dtaCycleCd: " + dtaCycleCd, e);
        }
    }
}
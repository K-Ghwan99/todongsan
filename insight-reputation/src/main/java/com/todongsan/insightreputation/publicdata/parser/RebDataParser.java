package com.todongsan.insightreputation.publicdata.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todongsan.insightreputation.enums.PublicDataSource;
import com.todongsan.insightreputation.enums.PublicDataType;
import com.todongsan.insightreputation.publicdata.dto.ParsedDataRow;
import com.todongsan.insightreputation.publicdata.dto.RebDataRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RebDataParser {

    private final ObjectMapper objectMapper;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    /**
     * REB 원본 데이터를 ParsedDataRow로 변환
     * 
     * @param rebDataRows REB API 응답 데이터
     * @param dataType 데이터 타입
     * @return 파싱된 데이터 목록
     */
    public List<ParsedDataRow> parseRebData(List<RebDataRow> rebDataRows, PublicDataType dataType) {
        List<ParsedDataRow> parsedData = new ArrayList<>();
        
        log.info("REB 데이터 파싱 시작: inputSize={}, dataType={}", rebDataRows.size(), dataType);
        
        for (RebDataRow row : rebDataRows) {
            try {
                ParsedDataRow parsed = parseRebDataRow(row, dataType);
                if (parsed != null) {
                    parsedData.add(parsed);
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
     * REB 데이터 단일 행 파싱
     */
    private ParsedDataRow parseRebDataRow(RebDataRow row, PublicDataType dataType) {
        // 필수 필드 검증
        if (row.getClsId() == null || row.getClsFullnm() == null || row.getWrttimeDesc() == null) {
            log.debug("필수 필드 누락으로 행 건너뜀: clsId={}, clsFullnm={}, wrttimeDesc={}", 
                     row.getClsId(), row.getClsFullnm(), row.getWrttimeDesc());
            return null;
        }

        try {
            // 1. region_sido 파싱: cls_fullnm의 첫 번째 '>' 이전 값
            String regionSido = parseRegionSido(row.getClsFullnm());
            
            // 2. source_region_id: cls_id 그대로 사용
            String sourceRegionId = row.getClsId();
            
            // 3. region_fullpath: cls_fullnm 그대로 사용
            String regionFullpath = row.getClsFullnm();
            
            // 4. reference_date: wrttime_desc 파싱 (YYYY.MM.DD → YYYY-MM-DD)
            LocalDate referenceDate = parseReferenceDate(row.getWrttimeDesc());
            
            // 5. numeric_value: dta_val (null 허용)
            
            // 6. raw_data: 원본 JSON 직렬화
            String rawData = objectMapper.writeValueAsString(row);

            return ParsedDataRow.builder()
                    .source(PublicDataSource.REB)
                    .dataType(dataType)
                    .referenceDate(referenceDate)
                    .regionSido(regionSido)
                    .sourceRegionId(sourceRegionId)
                    .regionFullpath(regionFullpath)
                    .numericValue(row.getDtaVal())
                    .rawData(rawData)
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
     * wrttime_desc를 LocalDate로 변환
     * 예: "2025.05.12" → 2025-05-12
     */
    private LocalDate parseReferenceDate(String wrttimeDesc) {
        if (wrttimeDesc == null || wrttimeDesc.trim().isEmpty()) {
            throw new IllegalArgumentException("wrttime_desc가 비어있음");
        }
        
        try {
            return LocalDate.parse(wrttimeDesc.trim(), DATE_FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException("wrttime_desc 파싱 실패: " + wrttimeDesc, e);
        }
    }
}
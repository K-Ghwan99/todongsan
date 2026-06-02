package com.todongsan.insightreputation.publicdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RebDataRow {
    
    @JsonProperty("CLS_ID")
    private Long clsId;             // 지역 고유 코드 (숫자 타입)
    
    @JsonProperty("CLS_FULLNM")
    private String clsFullnm;       // 지역 전체 경로 (예: 경기>경부1권>과천시)
    
    @JsonProperty("ITM_ID")
    private Long itmId;             // 항목 ID (숫자 타입으로 옴. itm_id 저장 시 String.valueOf() 변환)
    
    @JsonProperty("ITM_NM")
    private String itmNm;           // 항목명 (예: 지수, 변동률)
    
    @JsonProperty("DTA_VAL")
    private Double dtaVal;          // 수치값 (null 가능)
    
    @JsonProperty("WRTTIME_DESC")
    private String wrtimeDesc;      // 주간: "2012-05-07", 월간: "2003년 11월"
    
    @JsonProperty("DTACYCLE_CD")
    private String dtaCycleCd;      // "WK" 또는 "MM" - WRTTIME_DESC 파싱 분기에 사용
}
package com.todongsan.insightreputation.publicdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class RebDataRow {
    
    @JsonProperty("cls_id")
    private String clsId;           // 지역 고유 코드
    
    @JsonProperty("cls_fullnm")
    private String clsFullnm;       // 지역 전체 경로 (예: 서울>강북지역>도심권>종로구)
    
    @JsonProperty("dta_val")
    private BigDecimal dtaVal;      // 수치값
    
    @JsonProperty("wrttime_desc")
    private String wrttimeDesc;     // 기준일 (예: 2025.05.12)
    
    @JsonProperty("statbl_id")
    private String statblId;        // 통계표 ID
    
    @JsonProperty("period")
    private String period;          // 주기 (WK, MM)
}
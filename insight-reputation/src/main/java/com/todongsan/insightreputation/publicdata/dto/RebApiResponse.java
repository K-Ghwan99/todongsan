package com.todongsan.insightreputation.publicdata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
public class RebApiResponse {
    
    // 오류 응답: {"RESULT": {"CODE": "INFO-200", "MESSAGE": "..."}}
    @JsonProperty("RESULT")
    private ResultInfo directResult;
    
    // 정상 응답: {"SttsApiTblData": [...]}
    @JsonProperty("SttsApiTblData")
    private List<SttsApiTblDataItem> sttsApiTblData;
    
    @Getter
    @NoArgsConstructor
    public static class ResultInfo {
        @JsonProperty("CODE")
        private String resultCode;
        
        @JsonProperty("MESSAGE")
        private String resultMsg;
    }
    
    @Getter
    @NoArgsConstructor
    public static class SttsApiTblDataItem {
        @JsonProperty("head")
        private List<Object> head;
        
        @JsonProperty("row")
        private List<RebDataRow> row;
    }
    
    @Getter
    @NoArgsConstructor
    public static class HeadInfo {
        @JsonProperty("list_total_count")
        private Integer listTotalCount;
        
        @JsonProperty("RESULT")
        private ResultInfo result;
    }
    
    // 편의 메서드들
    public String getResultCode() {
        if (directResult != null) {
            return directResult.getResultCode();
        }
        
        if (sttsApiTblData != null && !sttsApiTblData.isEmpty()) {
            SttsApiTblDataItem firstItem = sttsApiTblData.get(0);
            if (firstItem.getHead() != null && !firstItem.getHead().isEmpty()) {
                // head의 두 번째 요소가 RESULT 정보
                for (Object headItem : firstItem.getHead()) {
                    if (headItem instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> map = (java.util.Map<String, Object>) headItem;
                        Object result = map.get("RESULT");
                        if (result instanceof java.util.Map) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> resultMap = (java.util.Map<String, Object>) result;
                            return (String) resultMap.get("CODE");
                        }
                    }
                }
            }
        }
        
        return "INFO-000"; // 기본값
    }
    
    public Integer getTotalCount() {
        if (sttsApiTblData != null && !sttsApiTblData.isEmpty()) {
            SttsApiTblDataItem firstItem = sttsApiTblData.get(0);
            if (firstItem.getHead() != null && !firstItem.getHead().isEmpty()) {
                // head의 첫 번째 요소가 총 건수 정보
                Object headItem = firstItem.getHead().get(0);
                if (headItem instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> map = (java.util.Map<String, Object>) headItem;
                    Object count = map.get("list_total_count");
                    if (count instanceof Number) {
                        return ((Number) count).intValue();
                    }
                }
            }
        }
        return null;
    }
    
    public List<RebDataRow> getRows() {
        if (sttsApiTblData != null && sttsApiTblData.size() > 1) {
            SttsApiTblDataItem secondItem = sttsApiTblData.get(1);
            if (secondItem.getRow() != null) {
                return secondItem.getRow();
            }
        }
        return Collections.emptyList();
    }
}
package com.todongsan.insightreputation.publicdata.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class RebApiResponse {
    
    private ResultInfo resultInfo;
    private List<RebDataRow> body;
    
    @Getter
    @NoArgsConstructor
    public static class ResultInfo {
        private String resultCode;
        private String resultMsg;
        private Integer totalCount;
        private Integer pageNo;
        private Integer numOfRows;
    }
}
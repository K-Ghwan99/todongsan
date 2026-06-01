package com.todongsan.insightreputation.global.client;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MarketPredictionsPageResponse {
    
    private List<MarketPredictionResponse> content;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
}
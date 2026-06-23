package com.todongsan.insightreputation.insight.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MarketPublicDataReferenceResponse {

    private String title;
    private String summary;
    private String content;
    private LocalDateTime dataAsOf;
}

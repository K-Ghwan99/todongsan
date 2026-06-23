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
    /** true: Claude AI 분석 결과, false: Claude 호출 실패 시 공공 데이터 원문 표로 대체 */
    private boolean aiAnalyzed;
}

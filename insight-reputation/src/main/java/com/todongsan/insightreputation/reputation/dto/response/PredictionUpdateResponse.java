package com.todongsan.insightreputation.reputation.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PredictionUpdateResponse {

    private Long memberId;
    private Integer predictionCount;
    private Integer predictionCorrect;
    private Double predictionAccuracy;
}
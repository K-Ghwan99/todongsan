package com.todongsan.memberpointservice.point.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BatchItemResult {

    private Long predictionId;
    private Long memberId;
    private String status;         // PROCESSED | ALREADY_PROCESSED | FAILED
    private String errorCode;
    private String amount;
    private String balanceSnapshot;
}

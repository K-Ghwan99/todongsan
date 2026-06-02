package com.todongsan.insightreputation.reputation.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActivityUpdateResponse {

    private Long memberId;
    private Integer activityScore;
    private Integer activityCount;
    private Boolean activityConfirmed;
}
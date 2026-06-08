package com.todongsan.memberpointservice.point.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SettlementResponse {

    private Long marketId;
    private List<BatchItemResult> results;
}

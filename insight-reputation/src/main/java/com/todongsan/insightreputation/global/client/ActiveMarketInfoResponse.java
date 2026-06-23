package com.todongsan.insightreputation.global.client;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ActiveMarketInfoResponse {

    private Long marketId;
    private String title;
    private List<String> optionLabels;
    private String regionSido;
    private String regionSigu;
}

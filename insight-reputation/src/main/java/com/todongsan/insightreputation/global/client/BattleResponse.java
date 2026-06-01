package com.todongsan.insightreputation.global.client;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BattleResponse {
    
    private Long battleId;
    private String title;
    private String sido;
    private String sigu;
    private String status;
}
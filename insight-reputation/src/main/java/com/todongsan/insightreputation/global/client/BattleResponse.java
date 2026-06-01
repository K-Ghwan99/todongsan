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
    private Boolean isClosed;    // Battle 종료 여부
    private String optionA;      // A 옵션 내용
    private String optionB;      // B 옵션 내용
}
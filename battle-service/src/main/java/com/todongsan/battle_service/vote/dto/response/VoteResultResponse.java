package com.todongsan.battle_service.vote.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VoteResultResponse {

    private Long battleId;
    private String status;
    private boolean voted;
    private boolean resultVisible;

    // resultVisible = true일 때만 포함
    private Integer optionACount;
    private Integer optionBCount;
    private Integer voteCount;
    private Double optionARatio;
    private Double optionBRatio;
    private String winningOption;

    // CLOSED + 72h 미경과 + 미투표인 경우 비공개
    private String message;
}

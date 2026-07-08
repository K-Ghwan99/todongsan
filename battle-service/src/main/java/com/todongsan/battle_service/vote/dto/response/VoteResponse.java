package com.todongsan.battle_service.vote.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VoteResponse {

    private Long battleId;
    private String selectedOption;
    private String message;
}

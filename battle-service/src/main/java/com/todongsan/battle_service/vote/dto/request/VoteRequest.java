package com.todongsan.battle_service.vote.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VoteRequest {

    @NotBlank
    private String option; // "A" or "B"
}

package com.todongsan.marketservice.market.repository;

import com.todongsan.marketservice.market.type.ReputationUpdateStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarketReputationUpdateRow {
    private Long id;
    private Long marketId;
    private Long predictionId;
    private Long memberId;
    private Boolean isCorrect;
    private ReputationUpdateStatus status;
    private Integer attemptNo;
    private String lastErrorCode;
    private String lastErrorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

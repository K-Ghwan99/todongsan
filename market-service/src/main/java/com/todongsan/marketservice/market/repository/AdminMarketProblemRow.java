package com.todongsan.marketservice.market.repository;

import com.todongsan.marketservice.market.type.AdminMarketProblemType;
import com.todongsan.marketservice.market.type.MarketStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminMarketProblemRow {
    private Long marketId;
    private String title;
    private MarketStatus marketStatus;
    private AdminMarketProblemType problemType;
    private long failedCount;
    private long unknownCount;
    private long pendingStaleCount;
    private String lastErrorCode;
    private String lastErrorMessage;
    private LocalDateTime lastAttemptAt;
}

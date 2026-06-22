package com.todongsan.marketservice.market.repository;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminMarketStatusCountsRow {
    private long total;
    private long pending;
    private long active;
    private long closedByTime;
    private long closed;
    private long dataPending;
    private long settlementInProgress;
    private long settled;
    private long voided;
}

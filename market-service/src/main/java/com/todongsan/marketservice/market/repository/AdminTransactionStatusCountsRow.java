package com.todongsan.marketservice.market.repository;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminTransactionStatusCountsRow {
    private long totalCount;
    private long successCount;
    private long failedCount;
    private long unknownCount;
    private long pendingCount;
    private BigDecimal totalAmount;
}

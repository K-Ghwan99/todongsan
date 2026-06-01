package com.todongsan.marketservice.market.entity;

import com.todongsan.marketservice.market.type.MarketAnswerType;
import com.todongsan.marketservice.market.type.MarketCategory;
import com.todongsan.marketservice.market.type.MarketMetricUnit;
import com.todongsan.marketservice.market.type.MarketPriceModel;
import com.todongsan.marketservice.market.type.MarketStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Market {
    private Long id;

    private String title;
    private String description;

    private MarketCategory category;
    private MarketAnswerType answerType;
    private MarketMetricUnit metricUnit;

    private String judgeDataSource;
    private String judgeCriteria;
    private LocalDate judgeDate;

    private MarketStatus status;

    private LocalDateTime closeAt;
    private LocalDateTime settleDueAt;
    private LocalDateTime settledAt;

    private Long resultOptionId;
    private BigDecimal resultValue;
    private String resultText;

    private BigDecimal totalPool;
    private BigDecimal feeRate;
    private BigDecimal feeAmount;
    private BigDecimal settlementPool;

    private BigDecimal initialVirtualLiquidity;
    private MarketPriceModel priceModel;

    private Long createdBy;

    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


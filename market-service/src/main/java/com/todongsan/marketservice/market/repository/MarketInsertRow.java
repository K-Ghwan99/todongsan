package com.todongsan.marketservice.market.repository;

import com.todongsan.marketservice.market.type.MarketAnswerType;
import com.todongsan.marketservice.market.type.MarketCategory;
import com.todongsan.marketservice.market.type.MarketMetricUnit;
import com.todongsan.marketservice.market.type.MarketPriceModel;
import com.todongsan.marketservice.market.type.MarketStatus;
import com.todongsan.marketservice.market.type.RegionScope;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MarketInsertRow {
    private Long id;
    private String title;
    private String description;
    private MarketCategory category;
    private MarketAnswerType answerType;
    private MarketMetricUnit metricUnit;
    private RegionScope regionScope;
    private String regionSido;
    private String regionSigu;
    private String judgeDataSource;
    private String judgeCriteria;
    private LocalDate judgeDate;
    private MarketStatus status;
    private LocalDateTime closeAt;
    private LocalDateTime settleDueAt;
    private BigDecimal totalPool;
    private BigDecimal feeRate;
    private BigDecimal feeAmount;
    private BigDecimal settlementPool;
    private BigDecimal initialVirtualLiquidity;
    private MarketPriceModel priceModel;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

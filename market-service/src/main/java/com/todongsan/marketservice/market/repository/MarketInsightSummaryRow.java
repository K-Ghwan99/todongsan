package com.todongsan.marketservice.market.repository;

import com.todongsan.marketservice.market.type.MarketAnswerType;
import com.todongsan.marketservice.market.type.MarketCategory;
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
public class MarketInsightSummaryRow {
    private Long marketId;
    private String title;
    private MarketCategory category;
    private MarketAnswerType answerType;
    private MarketStatus status;
    private LocalDateTime closeAt;
    private LocalDate judgeDate;
    private String judgeDataSource;
    private String judgeCriteria;
    private Long resultOptionId;
    private BigDecimal resultValue;
    private String resultText;
    private BigDecimal totalPoolAmount;
    private BigDecimal settlementPoolAmount;
    private LocalDateTime settledAt;
}

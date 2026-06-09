package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.todongsan.marketservice.global.serializer.BigDecimalPlainStringSerializer;
import com.todongsan.marketservice.market.type.MarketAnswerType;
import com.todongsan.marketservice.market.type.MarketCategory;
import com.todongsan.marketservice.market.type.MarketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MarketInsightMarketSummaryResponse(
        Long marketId,
        String title,
        MarketCategory category,
        MarketAnswerType answerType,
        MarketStatus status,
        LocalDateTime closeAt,
        LocalDate judgeDate,
        String judgeDataSource,
        String judgeCriteria,
        Long resultOptionId,
        @Schema(description = "확정 결과 값. Decimal String으로 응답", type = "string", example = "0.7500")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal resultValue,
        String resultText,
        Long totalPredictionCount,
        @Schema(description = "Insight 문맥의 실제 참여 포인트 총합. virtualPoolAmount를 포함하지 않음", type = "string", example = "600.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal totalPoolAmount,
        @Schema(description = "정산 대상 풀 금액. market_settlement.settlement_pool 기준", type = "string", example = "570.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal settlementPoolAmount,
        LocalDateTime settledAt
) {
}

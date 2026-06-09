package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.todongsan.marketservice.global.serializer.BigDecimalPlainStringSerializer;
import com.todongsan.marketservice.market.type.PriceHistoryEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarketPriceHistoryResponse {
    private List<PriceHistory> content;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private Boolean last;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceHistory {
        private Long historyId;
        private Long marketId;
        private Long optionId;
        private String optionContent;
        private Long predictionId;
        private PriceHistoryEventType eventType;

        @Schema(description = "변경 전 가격. Decimal String으로 응답", type = "string", example = "0.50000000")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        private BigDecimal priceBefore;

        @Schema(description = "변경 후 가격. Decimal String으로 응답", type = "string", example = "0.68750000")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        private BigDecimal priceAfter;

        @Schema(description = "가격 변화율. Decimal String으로 응답", type = "string", example = "0.37500000")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        private BigDecimal priceChangeRate;

        @Schema(description = "변경 전 실제 참여 풀. Decimal String으로 응답", type = "string", example = "0.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        private BigDecimal realPoolBefore;

        @Schema(description = "변경 후 실제 참여 풀. Decimal String으로 응답", type = "string", example = "100.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        private BigDecimal realPoolAfter;

        @Schema(description = "선택지 가상 유동성. 생성 후 수정되지 않는 값", type = "string", example = "100.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        private BigDecimal virtualPoolAmount;

        @Schema(description = "변경 전 계약 수량. Decimal String으로 응답", type = "string", example = "0.00000000")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        private BigDecimal contractQuantityBefore;

        @Schema(description = "변경 후 계약 수량. Decimal String으로 응답", type = "string", example = "200.00000000")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        private BigDecimal contractQuantityAfter;

        private LocalDateTime createdAt;
    }
}

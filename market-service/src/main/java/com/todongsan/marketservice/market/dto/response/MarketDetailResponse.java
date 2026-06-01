package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.todongsan.marketservice.market.type.MarketStatus;
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
public class MarketDetailResponse {
    private Long marketId;
    private String title;
    private String description;
    private MarketStatus status;

    private LocalDateTime closeAt;
    private LocalDateTime resultAnnounceAt;

    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal totalPoolAmount;
    private List<MarketOptionResponse> options;
}

package com.todongsan.marketservice.market.entity;

import com.todongsan.marketservice.market.type.MarketVoidReasonType;
import com.todongsan.marketservice.market.type.RefundStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarketVoid {
    private Long id;

    private Long marketId;

    private MarketVoidReasonType reasonType;
    private String reasonDetail;

    private RefundStatus refundStatus;

    private Long voidedBy;
    private LocalDateTime voidedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


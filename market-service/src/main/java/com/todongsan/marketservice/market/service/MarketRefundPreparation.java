package com.todongsan.marketservice.market.service;

import com.todongsan.marketservice.market.dto.response.RefundMarketResponse;
import com.todongsan.marketservice.market.entity.MarketRefundDetail;
import com.todongsan.marketservice.market.type.MarketStatus;
import com.todongsan.marketservice.market.type.RefundStatus;
import java.util.List;

record MarketRefundPreparation(
        Long marketId,
        Long voidId,
        List<MarketRefundDetail> refundDetails,
        boolean completed
) {

    RefundMarketResponse toResponse(
            int successCount,
            int failedCount,
            int unknownCount,
            RefundStatus refundStatus
    ) {
        return new RefundMarketResponse(
                marketId,
                voidId,
                refundDetails.size(),
                successCount,
                failedCount,
                unknownCount,
                MarketStatus.VOIDED,
                refundStatus
        );
    }
}

package com.todongsan.marketservice.market.dto.response;

import com.todongsan.marketservice.market.type.RegionScope;
import java.util.List;

public record MarketBasicInfoResponse(
        Long marketId,
        String title,
        List<String> optionLabels,
        RegionScope regionScope,
        String regionSido,
        String regionSigu
) {
}

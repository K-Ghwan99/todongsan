package com.todongsan.marketservice.market.dto.response;

import com.todongsan.marketservice.market.type.RegionScope;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateMarketResponse {
    private Long marketId;
    private RegionScope regionScope;
    private String regionSido;
    private String regionSigu;
}

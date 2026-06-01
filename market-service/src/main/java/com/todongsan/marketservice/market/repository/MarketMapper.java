package com.todongsan.marketservice.market.repository;

import com.todongsan.marketservice.market.entity.Market;
import com.todongsan.marketservice.market.entity.MarketOption;
import com.todongsan.marketservice.market.type.MarketStatus;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface MarketMapper {

    List<Market> selectMarkets(
            @Param("status") MarketStatus status,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    long countMarkets(
            @Param("status") MarketStatus status,
            @Param("keyword") String keyword
    );

    Market selectMarketById(@Param("marketId") long marketId);

    List<MarketOption> selectOptionsByMarketId(@Param("marketId") long marketId);

    List<MarketOption> selectOptionsByMarketIds(@Param("marketIds") List<Long> marketIds);

    List<MarketPriceHistoryRow> selectPriceHistory(
            @Param("marketId") long marketId,
            @Param("optionId") Long optionId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    long countPriceHistory(
            @Param("marketId") long marketId,
            @Param("optionId") Long optionId
    );
}

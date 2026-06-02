package com.todongsan.marketservice.market.repository;

import com.todongsan.marketservice.market.entity.Market;
import com.todongsan.marketservice.market.entity.MarketOption;
import com.todongsan.marketservice.market.entity.MarketPrediction;
import com.todongsan.marketservice.market.entity.MarketPriceHistory;
import com.todongsan.marketservice.market.type.MarketStatus;
import com.todongsan.marketservice.market.type.PredictionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    void insertMarket(MarketInsertRow market);

    void insertMarketOptions(@Param("options") List<MarketOptionInsertRow> options);

    int activatePendingMarket(
            @Param("marketId") long marketId,
            @Param("updatedAt") java.time.LocalDateTime updatedAt
    );

    MarketOption selectOptionById(@Param("optionId") long optionId);

    MarketPrediction selectPredictionByMarketIdAndMemberId(
            @Param("marketId") long marketId,
            @Param("memberId") long memberId
    );

    MarketPrediction lockPredictionByMarketIdAndMemberId(
            @Param("marketId") long marketId,
            @Param("memberId") long memberId
    );

    MarketPrediction selectPredictionById(@Param("predictionId") long predictionId);

    void insertPrediction(MarketPrediction prediction);

    int retryFailedPrediction(
            @Param("predictionId") long predictionId,
            @Param("optionId") long optionId,
            @Param("pointAmount") BigDecimal pointAmount,
            @Param("pointSpendIdempotencyKey") String pointSpendIdempotencyKey,
            @Param("attemptNo") int attemptNo,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    Market lockMarketById(@Param("marketId") long marketId);

    List<MarketOption> lockOptionsByMarketId(@Param("marketId") long marketId);

    void updateMarketTotalPool(
            @Param("marketId") long marketId,
            @Param("pointAmount") BigDecimal pointAmount,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    void updateMarketOptionPoolsAndPrice(MarketOption option);

    void insertPriceHistoryRows(@Param("histories") List<MarketPriceHistory> histories);

    int updatePredictionConfirmed(
            @Param("predictionId") long predictionId,
            @Param("priceSnapshot") BigDecimal priceSnapshot,
            @Param("contractQuantity") BigDecimal contractQuantity,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    int updatePendingPredictionStatus(
            @Param("predictionId") long predictionId,
            @Param("status") PredictionStatus status,
            @Param("failReason") String failReason,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}

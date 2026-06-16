package com.todongsan.marketservice.market.service;

import com.todongsan.marketservice.global.exception.CustomException;
import com.todongsan.marketservice.global.exception.errorcode.MarketErrorCode;
import com.todongsan.marketservice.market.dto.response.MarketDetailResponse;
import com.todongsan.marketservice.market.dto.response.MarketListResponse;
import com.todongsan.marketservice.market.dto.response.MarketOptionResponse;
import com.todongsan.marketservice.market.dto.response.MarketPriceHistoryResponse;
import com.todongsan.marketservice.market.entity.Market;
import com.todongsan.marketservice.market.entity.MarketOption;
import com.todongsan.marketservice.market.repository.MarketMapper;
import com.todongsan.marketservice.market.repository.MarketPriceHistoryRow;
import com.todongsan.marketservice.market.type.MarketDisplayStatus;
import com.todongsan.marketservice.market.type.MarketStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketService {

    private static final int RATE_SCALE = 8;
    private static final RoundingMode PRICE_HISTORY_ROUNDING = RoundingMode.HALF_UP;

    private final MarketMapper marketMapper;

    public MarketListResponse getMarkets(int page, int size, MarketStatus status, String keyword) {
        LocalDateTime now = LocalDateTime.now();
        int offset = page * size;
        List<Market> markets = marketMapper.selectMarkets(status, keyword, offset, size);
        List<Long> marketIds = markets.stream()
                .map(Market::getId)
                .toList();
        Map<Long, List<MarketOption>> optionsByMarketId = marketIds.isEmpty()
                ? Collections.emptyMap()
                : marketMapper.selectOptionsByMarketIds(marketIds).stream()
                        .collect(Collectors.groupingBy(MarketOption::getMarketId));

        List<MarketListResponse.MarketSummary> content = markets.stream()
                .map(market -> new MarketListResponse.MarketSummary(
                        market.getId(),
                        market.getTitle(),
                        market.getStatus(),
                        market.getCloseAt(),
                        market.getTotalPool(),
                        optionsByMarketId.getOrDefault(market.getId(), Collections.emptyList()).stream()
                                .map(this::toListOptionResponse)
                                .toList(),
                        canPredict(market, now),
                        displayStatus(market, now)
                ))
                .toList();
        long totalElements = marketMapper.countMarkets(status, keyword);

        return new MarketListResponse(
                content,
                page,
                size,
                totalElements,
                totalPages(totalElements, size),
                isLast(page, size, totalElements)
        );
    }

    public MarketDetailResponse getMarket(long marketId) {
        LocalDateTime now = LocalDateTime.now();
        Market market = findMarket(marketId);
        List<MarketOptionResponse> options = marketMapper.selectOptionsByMarketId(marketId).stream()
                .map(this::toDetailOptionResponse)
                .toList();

        return new MarketDetailResponse(
                market.getId(),
                market.getTitle(),
                market.getDescription(),
                market.getStatus(),
                market.getCloseAt(),
                market.getSettleDueAt(),
                market.getTotalPool(),
                options,
                canPredict(market, now),
                displayStatus(market, now)
        );
    }

    private boolean canPredict(Market market, LocalDateTime now) {
        return market.getStatus() == MarketStatus.ACTIVE
                && market.getCloseAt().isAfter(now);
    }

    private MarketDisplayStatus displayStatus(Market market, LocalDateTime now) {
        if (market.getStatus() == MarketStatus.ACTIVE
                && !market.getCloseAt().isAfter(now)) {
            return MarketDisplayStatus.CLOSED_BY_TIME;
        }
        return MarketDisplayStatus.valueOf(market.getStatus().name());
    }

    public MarketPriceHistoryResponse getPriceHistory(long marketId, int page, int size, Long optionId) {
        findMarket(marketId);
        validateOptionBelongsToMarket(marketId, optionId);
        int offset = page * size;
        List<MarketPriceHistoryResponse.PriceHistory> content = marketMapper
                .selectPriceHistory(marketId, optionId, offset, size)
                .stream()
                .map(this::toPriceHistoryResponse)
                .toList();
        long totalElements = marketMapper.countPriceHistory(marketId, optionId);

        return new MarketPriceHistoryResponse(
                content,
                page,
                size,
                totalElements,
                totalPages(totalElements, size),
                isLast(page, size, totalElements)
        );
    }

    private Market findMarket(long marketId) {
        Market market = marketMapper.selectMarketById(marketId);
        if (market == null) {
            throw new CustomException(MarketErrorCode.MARKET_NOT_FOUND);
        }
        return market;
    }

    private MarketOptionResponse toListOptionResponse(MarketOption option) {
        return new MarketOptionResponse(
                option.getId(),
                option.getOptionText(),
                option.getCurrentPrice(),
                null,
                null
        );
    }

    private MarketOptionResponse toDetailOptionResponse(MarketOption option) {
        return new MarketOptionResponse(
                option.getId(),
                option.getOptionText(),
                option.getCurrentPrice(),
                option.getRealPoolAmount(),
                option.getVirtualPoolAmount()
        );
    }

    private MarketPriceHistoryResponse.PriceHistory toPriceHistoryResponse(MarketPriceHistoryRow history) {
        return new MarketPriceHistoryResponse.PriceHistory(
                history.getHistoryId(),
                history.getMarketId(),
                history.getOptionId(),
                history.getOptionContent(),
                history.getPredictionId(),
                history.getEventType(),
                history.getPriceBefore(),
                history.getPriceAfter(),
                priceChangeRate(history.getPriceBefore(), history.getPriceAfter()),
                history.getRealPoolBefore(),
                history.getRealPoolAfter(),
                history.getVirtualPoolAmount(),
                history.getContractQuantityBefore(),
                history.getContractQuantityAfter(),
                history.getCreatedAt()
        );
    }

    private void validateOptionBelongsToMarket(long marketId, Long optionId) {
        if (optionId == null) {
            return;
        }
        boolean exists = marketMapper.selectOptionsByMarketId(marketId).stream()
                .anyMatch(option -> option.getId().equals(optionId));
        if (!exists) {
            throw new CustomException(MarketErrorCode.MARKET_OPTION_NOT_FOUND);
        }
    }

    private BigDecimal priceChangeRate(BigDecimal priceBefore, BigDecimal priceAfter) {
        if (priceBefore == null || priceAfter == null || priceBefore.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(MarketErrorCode.MARKET_INVALID_OPTION);
        }
        return priceAfter.subtract(priceBefore)
                .divide(priceBefore, RATE_SCALE, PRICE_HISTORY_ROUNDING)
                .multiply(new BigDecimal("100"))
                .setScale(RATE_SCALE, PRICE_HISTORY_ROUNDING);
    }

    private int totalPages(long totalElements, int size) {
        return (int) ((totalElements + size - 1) / size);
    }

    private boolean isLast(int page, int size, long totalElements) {
        return (long) (page + 1) * size >= totalElements;
    }
}

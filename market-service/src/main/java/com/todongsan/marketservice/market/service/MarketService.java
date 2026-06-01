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
import com.todongsan.marketservice.market.type.MarketStatus;
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

    private final MarketMapper marketMapper;

    public MarketListResponse getMarkets(int page, int size, MarketStatus status, String keyword) {
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
                                .toList()
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
                options
        );
    }

    public MarketPriceHistoryResponse getPriceHistory(long marketId, int page, int size, Long optionId) {
        findMarket(marketId);
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
                history.getOptionId(),
                history.getPrice(),
                history.getRealPoolAmount(),
                history.getVirtualPoolAmount(),
                history.getContractQuantity(),
                history.getCreatedAt()
        );
    }

    private int totalPages(long totalElements, int size) {
        return (int) ((totalElements + size - 1) / size);
    }

    private boolean isLast(int page, int size, long totalElements) {
        return (long) (page + 1) * size >= totalElements;
    }
}

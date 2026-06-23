package com.todongsan.marketservice.market.service;

import com.todongsan.marketservice.global.exception.CustomException;
import com.todongsan.marketservice.global.exception.errorcode.CommonErrorCode;
import com.todongsan.marketservice.global.exception.errorcode.MarketErrorCode;
import com.todongsan.marketservice.market.dto.response.MarketBasicInfoResponse;
import com.todongsan.marketservice.market.dto.response.MarketInsightMarketSummaryResponse;
import com.todongsan.marketservice.market.dto.response.MarketInsightOptionStatisticsResponse;
import com.todongsan.marketservice.market.dto.response.MarketInsightPredictionPageResponse;
import com.todongsan.marketservice.market.dto.response.MarketInsightPredictionResponse;
import com.todongsan.marketservice.market.dto.response.MarketInsightSummaryResponse;
import com.todongsan.marketservice.market.entity.Market;
import com.todongsan.marketservice.market.entity.MarketOption;
import com.todongsan.marketservice.market.repository.MarketInsightOptionStatisticsRow;
import com.todongsan.marketservice.market.repository.MarketInsightPredictionRow;
import com.todongsan.marketservice.market.repository.MarketInsightSummaryRow;
import com.todongsan.marketservice.market.repository.MarketMapper;
import com.todongsan.marketservice.market.type.MarketStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarketInsightService {

    private final MarketMapper marketMapper;

    @Value("${INTERNAL_AUTH_TOKEN:${internal.auth-token:}}")
    private String internalAuthToken;

    @Transactional(readOnly = true)
    public MarketBasicInfoResponse getBasicInfo(long marketId, String internalAuth) {
        validateInternalAuth(internalAuth);
        Market market = marketMapper.selectMarketBasicInfo(marketId);
        if (market == null) {
            throw new CustomException(MarketErrorCode.MARKET_NOT_FOUND);
        }
        List<String> optionLabels = marketMapper.selectOptionsByMarketId(marketId).stream()
                .map(MarketOption::getOptionText)
                .toList();

        return new MarketBasicInfoResponse(
                market.getId(),
                market.getTitle(),
                optionLabels,
                market.getRegionScope(),
                market.getRegionSido(),
                market.getRegionSigu()
        );
    }

    @Transactional(readOnly = true)
    public MarketInsightSummaryResponse getSummary(long marketId) {
        InsightMarketSource source = validateReadableInsightMarket(marketId);
        List<MarketInsightOptionStatisticsResponse> optionStatistics = marketMapper
                .selectInsightOptionStatistics(marketId)
                .stream()
                .map(this::toOptionStatisticsResponse)
                .toList();

        return new MarketInsightSummaryResponse(
                toMarketSummaryResponse(source.market(), source.totalPredictionCount()),
                optionStatistics
        );
    }

    @Transactional(readOnly = true)
    public MarketInsightPredictionPageResponse getPredictions(long marketId, int page, int size) {
        InsightMarketSource source = validateReadableInsightMarket(marketId);
        int offset = calculateOffset(page, size);
        List<MarketInsightPredictionResponse> content = marketMapper
                .selectInsightPredictions(marketId, offset, size)
                .stream()
                .map(this::toPredictionResponse)
                .toList();
        int totalPages = (int) Math.ceil((double) source.totalPredictionCount() / size);
        boolean last = page + 1 >= totalPages;

        return new MarketInsightPredictionPageResponse(
                content,
                page,
                size,
                source.totalPredictionCount(),
                totalPages,
                last
        );
    }

    private InsightMarketSource validateReadableInsightMarket(long marketId) {
        MarketInsightSummaryRow market = marketMapper.selectInsightMarketSummary(marketId);
        if (market == null) {
            throw new CustomException(MarketErrorCode.MARKET_NOT_FOUND);
        }
        if (market.getStatus() != MarketStatus.SETTLED) {
            throw new CustomException(MarketErrorCode.MARKET_INVALID_STATUS);
        }
        long predictionCount = marketMapper.countInsightPredictions(marketId);
        if (predictionCount == 0) {
            throw new CustomException(MarketErrorCode.MARKET_NO_PREDICTIONS);
        }
        return new InsightMarketSource(market, predictionCount);
    }

    private void validateInternalAuth(String internalAuth) {
        if (internalAuthToken == null || internalAuthToken.isBlank() || !internalAuthToken.equals(internalAuth)) {
            throw new CustomException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    private int calculateOffset(int page, int size) {
        long offset = (long) page * size;
        if (offset > Integer.MAX_VALUE) {
            throw new CustomException(CommonErrorCode.VALIDATION_FAILED);
        }
        return (int) offset;
    }

    private MarketInsightMarketSummaryResponse toMarketSummaryResponse(
            MarketInsightSummaryRow row,
            long totalPredictionCount
    ) {
        return new MarketInsightMarketSummaryResponse(
                row.getMarketId(),
                row.getTitle(),
                row.getCategory(),
                row.getAnswerType(),
                row.getStatus(),
                row.getCloseAt(),
                row.getJudgeDate(),
                row.getJudgeDataSource(),
                row.getJudgeCriteria(),
                row.getResultOptionId(),
                row.getResultValue(),
                row.getResultText(),
                totalPredictionCount,
                row.getTotalPoolAmount(),
                row.getSettlementPoolAmount(),
                row.getSettledAt()
        );
    }

    private MarketInsightOptionStatisticsResponse toOptionStatisticsResponse(MarketInsightOptionStatisticsRow row) {
        return new MarketInsightOptionStatisticsResponse(
                row.getOptionId(),
                row.getOptionCode(),
                row.getOptionLabel(),
                row.getRangeMin(),
                row.getRangeMax(),
                row.getMinInclusive(),
                row.getMaxInclusive(),
                row.getPredictionCount(),
                row.getParticipantCount(),
                row.getPoolAmount(),
                row.getFinalPrice(),
                row.getTotalContractQuantity(),
                row.getIsResult()
        );
    }

    private MarketInsightPredictionResponse toPredictionResponse(MarketInsightPredictionRow row) {
        return new MarketInsightPredictionResponse(
                row.getPredictionId(),
                row.getMemberId(),
                row.getOptionId(),
                row.getOptionCode(),
                row.getOptionLabel(),
                row.getPointAmount(),
                row.getPriceSnapshot(),
                row.getContractQuantity(),
                row.getStatus(),
                row.getIsCorrect(),
                row.getParticipatedAt()
        );
    }

    private record InsightMarketSource(MarketInsightSummaryRow market, long totalPredictionCount) {
    }
}

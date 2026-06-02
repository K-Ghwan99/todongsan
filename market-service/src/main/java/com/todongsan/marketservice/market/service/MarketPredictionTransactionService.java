package com.todongsan.marketservice.market.service;

import com.todongsan.marketservice.global.exception.CustomException;
import com.todongsan.marketservice.global.exception.errorcode.MarketErrorCode;
import com.todongsan.marketservice.market.dto.request.CreatePredictionRequest;
import com.todongsan.marketservice.market.dto.response.CreatePredictionResponse;
import com.todongsan.marketservice.market.entity.Market;
import com.todongsan.marketservice.market.entity.MarketOption;
import com.todongsan.marketservice.market.entity.MarketPrediction;
import com.todongsan.marketservice.market.entity.MarketPriceHistory;
import com.todongsan.marketservice.market.repository.MarketMapper;
import com.todongsan.marketservice.market.type.MarketStatus;
import com.todongsan.marketservice.market.type.PredictionStatus;
import com.todongsan.marketservice.market.type.PriceHistoryEventType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarketPredictionTransactionService {

    private static final BigDecimal MIN_POINT_AMOUNT = new BigDecimal("10.00");
    private static final BigDecimal MAX_POINT_AMOUNT = new BigDecimal("500.00");
    private static final int PRICE_SCALE = 8;

    private final MarketMapper marketMapper;

    @Transactional
    public MarketPrediction createPendingPrediction(
            long marketId,
            long memberId,
            String idempotencyKey,
            CreatePredictionRequest request
    ) {
        LocalDateTime now = LocalDateTime.now();
        Market market = getMarket(marketId);
        validateMarketForPrediction(market, now);
        MarketOption option = getMarketOption(marketId, request.getMarketOptionId());
        validatePointAmount(request.getPointAmount());
        if (marketMapper.selectPredictionByMarketIdAndMemberId(marketId, memberId) != null) {
            throw new CustomException(MarketErrorCode.MARKET_ALREADY_PREDICTED);
        }

        MarketPrediction prediction = new MarketPrediction();
        prediction.setMarketId(marketId);
        prediction.setOptionId(option.getId());
        prediction.setMemberId(memberId);
        prediction.setPointAmount(request.getPointAmount());
        prediction.setStatus(PredictionStatus.POINT_PENDING);
        prediction.setPointSpendIdempotencyKey(idempotencyKey);
        prediction.setCreatedAt(now);
        prediction.setUpdatedAt(now);
        marketMapper.insertPrediction(prediction);
        return prediction;
    }

    @Transactional
    public CreatePredictionResponse confirmPrediction(long predictionId) {
        MarketPrediction pendingPrediction = marketMapper.selectPredictionById(predictionId);
        if (pendingPrediction == null) {
            throw priceUpdateConflict();
        }

        Market market = marketMapper.lockMarketById(pendingPrediction.getMarketId());
        if (market == null) {
            throw new CustomException(MarketErrorCode.MARKET_NOT_FOUND);
        }
        List<MarketOption> options = marketMapper.lockOptionsByMarketId(market.getId());
        MarketPrediction prediction = marketMapper.selectPredictionById(predictionId);
        if (prediction == null || prediction.getStatus() != PredictionStatus.POINT_PENDING) {
            throw priceUpdateConflict();
        }

        MarketOption selectedOption = options.stream()
                .filter(option -> option.getId().equals(prediction.getOptionId()))
                .findFirst()
                .orElseThrow(() -> new CustomException(MarketErrorCode.MARKET_OPTION_NOT_FOUND));
        BigDecimal priceSnapshot = selectedOption.getCurrentPrice().setScale(PRICE_SCALE, RoundingMode.HALF_UP);
        if (priceSnapshot.compareTo(BigDecimal.ZERO) <= 0) {
            throw priceUpdateConflict();
        }
        BigDecimal contractQuantity = prediction.getPointAmount()
                .divide(priceSnapshot, PRICE_SCALE, RoundingMode.DOWN);
        LocalDateTime now = LocalDateTime.now();

        Map<Long, OptionSnapshot> snapshots = options.stream()
                .collect(Collectors.toMap(MarketOption::getId, OptionSnapshot::from));
        selectedOption.setRealPoolAmount(selectedOption.getRealPoolAmount().add(prediction.getPointAmount()));
        selectedOption.setTotalContractQuantity(
                selectedOption.getTotalContractQuantity().add(contractQuantity)
        );
        selectedOption.setPredictionCount(selectedOption.getPredictionCount() + 1);

        BigDecimal totalEffectivePool = options.stream()
                .map(this::effectivePool)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalEffectivePool.compareTo(BigDecimal.ZERO) <= 0) {
            throw priceUpdateConflict();
        }

        for (MarketOption option : options) {
            option.setCurrentPrice(effectivePool(option).divide(totalEffectivePool, PRICE_SCALE, RoundingMode.HALF_UP));
            option.setUpdatedAt(now);
        }
        marketMapper.updateMarketTotalPool(market.getId(), prediction.getPointAmount(), now);
        for (MarketOption option : options) {
            marketMapper.updateMarketOptionPoolsAndPrice(option);
        }
        marketMapper.insertPriceHistoryRows(options.stream()
                .map(option -> toPriceHistory(predictionId, option, snapshots.get(option.getId()), now))
                .toList());

        int updatedRows = marketMapper.updatePredictionConfirmed(
                predictionId,
                priceSnapshot,
                contractQuantity,
                now
        );
        if (updatedRows != 1) {
            throw priceUpdateConflict();
        }
        return new CreatePredictionResponse(
                predictionId,
                market.getId(),
                selectedOption.getId(),
                prediction.getPointAmount(),
                priceSnapshot,
                contractQuantity,
                PredictionStatus.CONFIRMED
        );
    }

    @Transactional
    public MarketPrediction markPredictionFailed(long predictionId, String failReason) {
        return updatePendingPredictionStatus(predictionId, PredictionStatus.FAILED, failReason);
    }

    @Transactional
    public MarketPrediction markPredictionUnknown(long predictionId, String failReason) {
        return updatePendingPredictionStatus(predictionId, PredictionStatus.POINT_UNKNOWN, failReason);
    }

    private MarketPrediction updatePendingPredictionStatus(
            long predictionId,
            PredictionStatus status,
            String failReason
    ) {
        int updatedRows = marketMapper.updatePendingPredictionStatus(
                predictionId,
                status,
                failReason,
                LocalDateTime.now()
        );
        MarketPrediction prediction = marketMapper.selectPredictionById(predictionId);
        if (prediction == null || (updatedRows != 1 && prediction.getStatus() != status)) {
            throw priceUpdateConflict();
        }
        return prediction;
    }

    private Market getMarket(long marketId) {
        Market market = marketMapper.selectMarketById(marketId);
        if (market == null) {
            throw new CustomException(MarketErrorCode.MARKET_NOT_FOUND);
        }
        return market;
    }

    private void validateMarketForPrediction(Market market, LocalDateTime now) {
        if (market.getStatus() != MarketStatus.ACTIVE) {
            throw new CustomException(MarketErrorCode.MARKET_NOT_ACTIVE);
        }
        if (!market.getCloseAt().isAfter(now)) {
            throw new CustomException(MarketErrorCode.MARKET_CLOSED);
        }
    }

    private MarketOption getMarketOption(long marketId, Long optionId) {
        if (optionId == null) {
            throw new CustomException(MarketErrorCode.MARKET_OPTION_NOT_FOUND);
        }
        MarketOption option = marketMapper.selectOptionById(optionId);
        if (option == null || !option.getMarketId().equals(marketId)) {
            throw new CustomException(MarketErrorCode.MARKET_OPTION_NOT_FOUND);
        }
        return option;
    }

    private void validatePointAmount(BigDecimal pointAmount) {
        if (pointAmount == null
                || pointAmount.scale() > 2
                || pointAmount.compareTo(MIN_POINT_AMOUNT) < 0
                || pointAmount.compareTo(MAX_POINT_AMOUNT) > 0) {
            throw new CustomException(MarketErrorCode.MARKET_INVALID_BET_AMOUNT);
        }
    }

    private BigDecimal effectivePool(MarketOption option) {
        return option.getRealPoolAmount().add(option.getVirtualPoolAmount());
    }

    private MarketPriceHistory toPriceHistory(
            long predictionId,
            MarketOption option,
            OptionSnapshot snapshot,
            LocalDateTime now
    ) {
        MarketPriceHistory history = new MarketPriceHistory();
        history.setMarketId(option.getMarketId());
        history.setOptionId(option.getId());
        history.setPredictionId(predictionId);
        history.setPriceBefore(snapshot.price());
        history.setPriceAfter(option.getCurrentPrice());
        history.setRealPoolBefore(snapshot.realPoolAmount());
        history.setRealPoolAfter(option.getRealPoolAmount());
        history.setContractQuantityBefore(snapshot.contractQuantity());
        history.setContractQuantityAfter(option.getTotalContractQuantity());
        history.setEventType(PriceHistoryEventType.PREDICTION_CONFIRMED);
        history.setCreatedAt(now);
        history.setUpdatedAt(now);
        return history;
    }

    private CustomException priceUpdateConflict() {
        return new CustomException(MarketErrorCode.MARKET_PRICE_UPDATE_CONFLICT);
    }

    private record OptionSnapshot(
            BigDecimal price,
            BigDecimal realPoolAmount,
            BigDecimal contractQuantity
    ) {
        private static OptionSnapshot from(MarketOption option) {
            return new OptionSnapshot(
                    option.getCurrentPrice(),
                    option.getRealPoolAmount(),
                    option.getTotalContractQuantity()
            );
        }
    }
}

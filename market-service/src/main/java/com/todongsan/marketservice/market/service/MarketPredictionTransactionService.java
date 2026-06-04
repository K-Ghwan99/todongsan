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
import java.util.Set;
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
        MarketPrediction prediction = marketMapper.lockPredictionByMarketIdAndMemberId(marketId, memberId);
        if (prediction != null) {
            return retryFailedPrediction(prediction, option, request, idempotencyKey, now);
        }

        prediction = new MarketPrediction();
        prediction.setMarketId(marketId);
        prediction.setOptionId(option.getId());
        prediction.setMemberId(memberId);
        prediction.setPointAmount(request.getPointAmount());
        prediction.setStatus(PredictionStatus.POINT_PENDING);
        prediction.setAttemptNo(1);
        prediction.setPointSpendIdempotencyKey(pointSpendIdempotencyKey(idempotencyKey, prediction.getAttemptNo()));
        prediction.setCreatedAt(now);
        prediction.setUpdatedAt(now);
        marketMapper.insertPrediction(prediction);
        return prediction;
    }

    private MarketPrediction retryFailedPrediction(
            MarketPrediction prediction,
            MarketOption option,
            CreatePredictionRequest request,
            String idempotencyKey,
            LocalDateTime now
    ) {
        if (prediction.getStatus() != PredictionStatus.FAILED) {
            throw new CustomException(MarketErrorCode.MARKET_ALREADY_PREDICTED);
        }
        int nextAttemptNo = prediction.getAttemptNo() + 1;
        int updatedRows = marketMapper.retryFailedPrediction(
                prediction.getId(),
                option.getId(),
                request.getPointAmount(),
                pointSpendIdempotencyKey(idempotencyKey, nextAttemptNo),
                nextAttemptNo,
                now
        );
        if (updatedRows != 1) {
            throw new CustomException(MarketErrorCode.MARKET_ALREADY_PREDICTED);
        }
        return marketMapper.selectPredictionById(prediction.getId());
    }

    private String pointSpendIdempotencyKey(String idempotencyKey, int attemptNo) {
        return "%s:attempt:%d".formatted(idempotencyKey, attemptNo);
    }

    @Transactional
    public CreatePredictionResponse confirmPrediction(long predictionId) {
        PredictionConfirmationResult result = confirmPredictionInternal(
                predictionId,
                Set.of(PredictionStatus.POINT_PENDING),
                Set.of(MarketStatus.ACTIVE),
                false
        );
        if (!result.confirmed()) {
            throw priceUpdateConflict();
        }
        return result.response();
    }

    @Transactional(readOnly = true)
    public List<MarketPrediction> selectPredictionsForSpendReconciliation(
            LocalDateTime pendingThreshold,
            int limit
    ) {
        return marketMapper.selectPredictionsForSpendReconciliation(pendingThreshold, limit);
    }

    @Transactional
    public boolean confirmPredictionForReconciliation(long predictionId) {
        return confirmPredictionInternal(
                predictionId,
                Set.of(PredictionStatus.POINT_PENDING, PredictionStatus.POINT_UNKNOWN),
                Set.of(MarketStatus.ACTIVE, MarketStatus.DATA_PENDING),
                true
        ).confirmed();
    }

    @Transactional
    public MarketPrediction markPredictionFailed(long predictionId, String failReason) {
        return updatePendingPredictionStatus(predictionId, PredictionStatus.FAILED, failReason);
    }

    @Transactional
    public MarketPrediction markPredictionUnknown(long predictionId, String failReason) {
        return updatePendingPredictionStatus(predictionId, PredictionStatus.POINT_UNKNOWN, failReason);
    }

    @Transactional
    public boolean failPredictionForReconciliation(long predictionId, String failReason) {
        return marketMapper.updatePredictionFailedForReconciliation(
                predictionId,
                failReason,
                LocalDateTime.now()
        ) == 1;
    }

    @Transactional
    public ReconciliationStatusUpdateResult markPredictionUnknownForReconciliation(
            long predictionId,
            String failReason
    ) {
        int updatedRows = marketMapper.updatePredictionUnknownFromPending(
                predictionId,
                failReason,
                LocalDateTime.now()
        );
        if (updatedRows == 1) {
            return ReconciliationStatusUpdateResult.UPDATED;
        }
        MarketPrediction prediction = marketMapper.selectPredictionById(predictionId);
        if (prediction != null && prediction.getStatus() == PredictionStatus.POINT_UNKNOWN) {
            return ReconciliationStatusUpdateResult.ALREADY_TARGET_STATUS;
        }
        return ReconciliationStatusUpdateResult.SKIPPED;
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

    private PredictionConfirmationResult confirmPredictionInternal(
            long predictionId,
            Set<PredictionStatus> allowedPredictionStatuses,
            Set<MarketStatus> allowedMarketStatuses,
            boolean skipOnInvalidState
    ) {
        MarketPrediction initialPrediction = marketMapper.selectPredictionById(predictionId);
        if (initialPrediction == null || hasMissingReconciliationData(initialPrediction)) {
            return confirmationFailure(skipOnInvalidState);
        }

        Market market = marketMapper.lockMarketById(initialPrediction.getMarketId());
        if (market == null) {
            if (skipOnInvalidState) {
                return PredictionConfirmationResult.skipped();
            }
            throw new CustomException(MarketErrorCode.MARKET_NOT_FOUND);
        }
        if (!allowedMarketStatuses.contains(market.getStatus())) {
            return confirmationFailure(skipOnInvalidState);
        }

        List<MarketOption> options = marketMapper.lockOptionsByMarketId(market.getId());
        MarketPrediction prediction = marketMapper.lockPredictionById(predictionId);
        if (prediction == null
                || hasMissingReconciliationData(prediction)
                || !allowedPredictionStatuses.contains(prediction.getStatus())) {
            return confirmationFailure(skipOnInvalidState);
        }

        MarketOption selectedOption = options.stream()
                .filter(option -> option.getId().equals(prediction.getOptionId()))
                .findFirst()
                .orElse(null);
        if (selectedOption == null) {
            if (skipOnInvalidState) {
                return PredictionConfirmationResult.skipped();
            }
            throw new CustomException(MarketErrorCode.MARKET_OPTION_NOT_FOUND);
        }
        BigDecimal priceSnapshot = selectedOption.getCurrentPrice().setScale(PRICE_SCALE, RoundingMode.HALF_UP);
        if (priceSnapshot.compareTo(BigDecimal.ZERO) <= 0) {
            return confirmationFailure(skipOnInvalidState);
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
            return confirmationFailure(skipOnInvalidState);
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

        int updatedRows = skipOnInvalidState
                ? marketMapper.updatePredictionConfirmedForReconciliation(
                        predictionId,
                        priceSnapshot,
                        contractQuantity,
                        now
                )
                : marketMapper.updatePredictionConfirmed(
                        predictionId,
                        priceSnapshot,
                        contractQuantity,
                        now
                );
        if (updatedRows != 1) {
            return confirmationFailure(skipOnInvalidState);
        }
        return PredictionConfirmationResult.confirmed(new CreatePredictionResponse(
                predictionId,
                market.getId(),
                selectedOption.getId(),
                prediction.getPointAmount(),
                priceSnapshot,
                contractQuantity,
                PredictionStatus.CONFIRMED
        ));
    }

    private PredictionConfirmationResult confirmationFailure(boolean skipOnInvalidState) {
        if (skipOnInvalidState) {
            return PredictionConfirmationResult.skipped();
        }
        throw priceUpdateConflict();
    }

    private boolean hasMissingReconciliationData(MarketPrediction prediction) {
        return prediction.getMarketId() == null
                || prediction.getOptionId() == null
                || prediction.getMemberId() == null
                || prediction.getPointAmount() == null;
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

    private record PredictionConfirmationResult(
            boolean confirmed,
            CreatePredictionResponse response
    ) {

        private static PredictionConfirmationResult confirmed(CreatePredictionResponse response) {
            return new PredictionConfirmationResult(true, response);
        }

        private static PredictionConfirmationResult skipped() {
            return new PredictionConfirmationResult(false, null);
        }
    }

    public enum ReconciliationStatusUpdateResult {
        UPDATED,
        ALREADY_TARGET_STATUS,
        SKIPPED
    }
}

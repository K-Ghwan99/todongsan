package com.todongsan.marketservice.market.service;

import com.todongsan.marketservice.market.type.PredictionStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class MarketPredictionPayoutEstimateCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int AMOUNT_SCALE = 2;
    private static final int QUANTITY_SCALE = 8;
    private static final int RATE_SCALE = 2;

    public PredictionPayoutEstimate calculate(
            PredictionStatus predictionStatus,
            BigDecimal pointAmount,
            BigDecimal contractQuantity,
            BigDecimal feeRate,
            BigDecimal estimateBaseTotalPool,
            BigDecimal estimateBaseOptionPointAmount,
            BigDecimal estimateBaseOptionContractQuantity
    ) {
        if (predictionStatus != PredictionStatus.CONFIRMED
                || pointAmount == null
                || pointAmount.compareTo(BigDecimal.ZERO) <= 0
                || contractQuantity == null
                || feeRate == null
                || estimateBaseTotalPool == null
                || estimateBaseOptionPointAmount == null
                || estimateBaseOptionContractQuantity == null
                || estimateBaseOptionContractQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return PredictionPayoutEstimate.empty();
        }

        BigDecimal totalPool = floorAmount(estimateBaseTotalPool);
        BigDecimal optionPrincipalPool = floorAmount(estimateBaseOptionPointAmount);
        BigDecimal losingPool = floorAmount(totalPool.subtract(optionPrincipalPool));
        if (losingPool.compareTo(BigDecimal.ZERO) < 0) {
            return PredictionPayoutEstimate.empty();
        }
        BigDecimal feeAmount = floorAmount(
                losingPool.multiply(feeRate).divide(HUNDRED, AMOUNT_SCALE, RoundingMode.DOWN)
        );
        BigDecimal rewardPool = floorAmount(losingPool.subtract(feeAmount));
        if (rewardPool.compareTo(BigDecimal.ZERO) < 0) {
            return PredictionPayoutEstimate.empty();
        }

        BigDecimal optionContractQuantity = estimateBaseOptionContractQuantity
                .setScale(QUANTITY_SCALE, RoundingMode.DOWN);
        BigDecimal rewardPerContract = rewardPool.divide(
                optionContractQuantity,
                QUANTITY_SCALE,
                RoundingMode.DOWN
        );
        BigDecimal estimatedPayout = floorAmount(pointAmount.add(contractQuantity.multiply(rewardPerContract)));
        BigDecimal estimatedProfit = floorAmount(estimatedPayout.subtract(pointAmount));
        BigDecimal estimatedProfitRate = estimatedProfit.multiply(HUNDRED)
                .divide(pointAmount, RATE_SCALE, RoundingMode.DOWN);

        return new PredictionPayoutEstimate(
                estimatedPayout,
                estimatedProfit,
                estimatedProfitRate,
                rewardPerContract,
                totalPool,
                rewardPool,
                optionContractQuantity
        );
    }

    private BigDecimal floorAmount(BigDecimal amount) {
        return amount.setScale(AMOUNT_SCALE, RoundingMode.DOWN);
    }

    public record PredictionPayoutEstimate(
            BigDecimal estimatedPayoutIfWin,
            BigDecimal estimatedProfitIfWin,
            BigDecimal estimatedProfitRateIfWin,
            BigDecimal currentPayoutPerContract,
            BigDecimal estimateBaseTotalPool,
            BigDecimal estimateBaseSettlementPool,
            BigDecimal estimateBaseOptionContractQuantity
    ) {
        private static PredictionPayoutEstimate empty() {
            return new PredictionPayoutEstimate(null, null, null, null, null, null, null);
        }
    }
}

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
            BigDecimal estimateBaseOptionContractQuantity
    ) {
        if (predictionStatus != PredictionStatus.CONFIRMED
                || pointAmount == null
                || pointAmount.compareTo(BigDecimal.ZERO) <= 0
                || contractQuantity == null
                || feeRate == null
                || estimateBaseTotalPool == null
                || estimateBaseOptionContractQuantity == null
                || estimateBaseOptionContractQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return PredictionPayoutEstimate.empty();
        }

        BigDecimal totalPool = floorAmount(estimateBaseTotalPool);
        BigDecimal feeAmount = floorAmount(
                totalPool.multiply(feeRate).divide(HUNDRED, AMOUNT_SCALE, RoundingMode.DOWN)
        );
        BigDecimal settlementPool = floorAmount(totalPool.subtract(feeAmount));
        if (settlementPool.compareTo(BigDecimal.ZERO) < 0) {
            return PredictionPayoutEstimate.empty();
        }

        BigDecimal optionContractQuantity = estimateBaseOptionContractQuantity
                .setScale(QUANTITY_SCALE, RoundingMode.DOWN);
        BigDecimal payoutPerContract = settlementPool.divide(
                optionContractQuantity,
                QUANTITY_SCALE,
                RoundingMode.DOWN
        );
        BigDecimal estimatedPayout = floorAmount(contractQuantity.multiply(payoutPerContract));
        BigDecimal estimatedProfit = floorAmount(estimatedPayout.subtract(pointAmount));
        BigDecimal estimatedProfitRate = estimatedProfit.multiply(HUNDRED)
                .divide(pointAmount, RATE_SCALE, RoundingMode.DOWN);

        return new PredictionPayoutEstimate(
                estimatedPayout,
                estimatedProfit,
                estimatedProfitRate,
                payoutPerContract,
                totalPool,
                settlementPool,
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

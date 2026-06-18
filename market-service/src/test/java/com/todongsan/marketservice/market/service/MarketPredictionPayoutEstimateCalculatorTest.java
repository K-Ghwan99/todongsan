package com.todongsan.marketservice.market.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.todongsan.marketservice.market.service.MarketPredictionPayoutEstimateCalculator.PredictionPayoutEstimate;
import com.todongsan.marketservice.market.type.PredictionStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MarketPredictionPayoutEstimateCalculatorTest {

    private final MarketPredictionPayoutEstimateCalculator calculator =
            new MarketPredictionPayoutEstimateCalculator();

    @Test
    void returnsPrincipalWhenThereIsNoLosingPool() {
        PredictionPayoutEstimate estimate = calculate(
                "10.00", "20.00000000", "10.00", "10.00", "20.00000000"
        );

        assertThat(estimate.estimatedPayoutIfWin()).isEqualByComparingTo("10.00");
        assertThat(estimate.estimatedProfitIfWin()).isEqualByComparingTo("0.00");
        assertThat(estimate.estimatedProfitRateIfWin()).isEqualByComparingTo("0.00");
        assertThat(estimate.currentPayoutPerContract()).isEqualByComparingTo("0.00000000");
        assertThat(estimate.estimateBaseSettlementPool()).isEqualByComparingTo("0.00");
    }

    @Test
    void givesMoreEstimatedRewardToEarlierParticipantWithMoreContracts() {
        PredictionPayoutEstimate early = calculate(
                "10.00", "20.00000000", "120.00", "20.00", "31.11111111"
        );
        PredictionPayoutEstimate late = calculate(
                "10.00", "11.11111111", "120.00", "20.00", "31.11111111"
        );

        assertThat(early.estimatedPayoutIfWin()).isEqualByComparingTo("71.07");
        assertThat(late.estimatedPayoutIfWin()).isEqualByComparingTo("43.92");
        assertThat(early.estimatedProfitIfWin()).isGreaterThan(late.estimatedProfitIfWin());
        assertThat(early.estimateBaseSettlementPool()).isEqualByComparingTo("95.00");
    }

    private PredictionPayoutEstimate calculate(
            String pointAmount,
            String contractQuantity,
            String totalPool,
            String optionPrincipalPool,
            String optionContractQuantity
    ) {
        return calculator.calculate(
                PredictionStatus.CONFIRMED,
                new BigDecimal(pointAmount),
                new BigDecimal(contractQuantity),
                new BigDecimal("5.00"),
                new BigDecimal(totalPool),
                new BigDecimal(optionPrincipalPool),
                new BigDecimal(optionContractQuantity)
        );
    }
}

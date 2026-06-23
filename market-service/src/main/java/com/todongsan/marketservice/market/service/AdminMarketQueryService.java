package com.todongsan.marketservice.market.service;

import com.todongsan.marketservice.global.exception.CustomException;
import com.todongsan.marketservice.global.exception.errorcode.CommonErrorCode;
import com.todongsan.marketservice.global.exception.errorcode.MarketErrorCode;
import com.todongsan.marketservice.market.dto.response.AdminMarketDetailResponse;
import com.todongsan.marketservice.market.dto.response.AdminMarketProblemPageResponse;
import com.todongsan.marketservice.market.dto.response.AdminMarketRefundDetailPageResponse;
import com.todongsan.marketservice.market.dto.response.AdminMarketRefundResponse;
import com.todongsan.marketservice.market.dto.response.AdminMarketSettlementDetailPageResponse;
import com.todongsan.marketservice.market.dto.response.AdminMarketSettlementResponse;
import com.todongsan.marketservice.market.dto.response.AdminMarketStatusCountsResponse;
import com.todongsan.marketservice.market.dto.response.AdminRefundDetailResponse;
import com.todongsan.marketservice.market.dto.response.AdminSettlementDetailResponse;
import com.todongsan.marketservice.market.entity.Market;
import com.todongsan.marketservice.market.entity.MarketOption;
import com.todongsan.marketservice.market.entity.MarketSettlement;
import com.todongsan.marketservice.market.entity.MarketVoid;
import com.todongsan.marketservice.market.repository.AdminMarketProblemRow;
import com.todongsan.marketservice.market.repository.AdminMarketStatusCountsRow;
import com.todongsan.marketservice.market.repository.AdminRefundDetailRow;
import com.todongsan.marketservice.market.repository.AdminSettlementDetailRow;
import com.todongsan.marketservice.market.repository.AdminTransactionStatusCountsRow;
import com.todongsan.marketservice.market.repository.MarketMapper;
import com.todongsan.marketservice.market.type.AdminMarketProblemStatus;
import com.todongsan.marketservice.market.type.AdminMarketProblemType;
import com.todongsan.marketservice.market.type.MarketDisplayStatus;
import com.todongsan.marketservice.market.type.MarketStatus;
import com.todongsan.marketservice.market.type.RefundStatus;
import com.todongsan.marketservice.market.type.TransactionItemStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMarketQueryService {

    private static final BigDecimal ZERO_AMOUNT = new BigDecimal("0.00");
    private static final BigDecimal ZERO_QUANTITY = new BigDecimal("0.00000000");
    private static final int RATE_SCALE = 8;
    private static final int STALE_MINUTES = 3;

    private final MarketMapper marketMapper;

    public AdminMarketDetailResponse getMarket(long marketId) {
        LocalDateTime now = LocalDateTime.now();
        Market market = findMarket(marketId);
        List<MarketOption> options = marketMapper.selectOptionsByMarketId(marketId);
        BigDecimal totalVirtualPool = options.stream()
                .map(MarketOption::getVirtualPoolAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalVirtualPool.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(MarketErrorCode.MARKET_INVALID_OPTION);
        }

        BigDecimal totalRealPool = amount(market.getTotalPool());
        List<AdminMarketDetailResponse.AdminMarketOption> optionResponses = options.stream()
                .map(option -> toOption(option, totalVirtualPool))
                .toList();
        long totalPredictionCount = options.stream()
                .map(MarketOption::getPredictionCount)
                .mapToLong(value -> value == null ? 0 : value)
                .sum();

        return new AdminMarketDetailResponse(
                market.getId(),
                market.getTitle(),
                market.getDescription(),
                market.getCategory(),
                market.getAnswerType(),
                market.getMetricUnit(),
                market.getRegionScope(),
                market.getRegionSido(),
                market.getRegionSigu(),
                market.getStatus(),
                displayStatus(market, now),
                canPredict(market, now),
                market.getPriceModel(),
                market.getCloseAt(),
                market.getJudgeDate(),
                market.getSettleDueAt(),
                market.getSettledAt(),
                market.getFeeRate(),
                amount(market.getFeeAmount()),
                amount(market.getSettlementPool()),
                market.getJudgeDataSource(),
                market.getJudgeCriteria(),
                market.getResultOptionId(),
                market.getResultValue(),
                market.getResultText(),
                totalRealPool,
                totalVirtualPool,
                totalRealPool.add(totalVirtualPool),
                totalPredictionCount,
                optionResponses,
                settlementSummary(marketId),
                refundSummary(market)
        );
    }

    public AdminMarketProblemPageResponse getProblemMarkets(int page, int size, String typeValue) {
        AdminMarketProblemType type = AdminMarketProblemType.from(typeValue);
        LocalDateTime pendingThreshold = LocalDateTime.now().minusMinutes(STALE_MINUTES);
        long totalElements = marketMapper.countProblemMarketsForAdmin(type, pendingThreshold);
        List<AdminMarketProblemPageResponse.Problem> content = marketMapper.selectProblemMarketsForAdmin(
                        type,
                        pendingThreshold,
                        page * size,
                        size
                ).stream()
                .map(this::toProblem)
                .toList();
        return new AdminMarketProblemPageResponse(
                content,
                page,
                size,
                totalElements,
                totalPages(totalElements, size),
                isLast(page, size, totalElements)
        );
    }

    public AdminMarketSettlementResponse getSettlement(long marketId) {
        Market market = findMarket(marketId);
        MarketSettlement settlement = marketMapper.selectLatestMarketSettlementByMarketIdForRead(marketId);
        if (settlement == null) {
            return emptySettlement(market);
        }
        AdminTransactionStatusCountsRow counts = marketMapper.selectSettlementDetailStatusCounts(settlement.getId());
        MarketOption resultOption = marketMapper.selectOptionById(settlement.getResultOptionId());
        return new AdminMarketSettlementResponse(
                market.getId(),
                market.getTitle(),
                market.getStatus(),
                settlement.getId(),
                settlement.getStatus(),
                settlement.getResultOptionId(),
                resultOption == null ? null : resultOption.getOptionText(),
                settlement.getTotalPool(),
                settlement.getFeeRate(),
                settlement.getFeeAmount(),
                settlement.getSettlementPool(),
                settlement.getWinningContractQuantity(),
                settlement.getPayoutPerContract(),
                settlement.getBurnedPointAmount(),
                counts.getTotalCount(),
                counts.getSuccessCount(),
                counts.getFailedCount(),
                counts.getUnknownCount(),
                counts.getPendingCount(),
                settlement.getCreatedAt(),
                settlement.getUpdatedAt()
        );
    }

    public AdminMarketSettlementDetailPageResponse getSettlementDetails(
            long marketId,
            long settlementId,
            int page,
            int size,
            String statusValue
    ) {
        findMarket(marketId);
        MarketSettlement settlement = marketMapper.selectMarketSettlementByIdForRead(settlementId);
        if (settlement == null || !Long.valueOf(marketId).equals(settlement.getMarketId())) {
            throw new CustomException(MarketErrorCode.MARKET_INVALID_SETTLEMENT_DATA);
        }
        TransactionItemStatus status = transactionStatus(statusValue);
        long totalElements = marketMapper.countSettlementDetailsForAdmin(settlementId, status);
        List<AdminSettlementDetailResponse> content = marketMapper.selectSettlementDetailsForAdmin(
                        settlementId,
                        status,
                        page * size,
                        size
                ).stream()
                .map(this::toSettlementDetail)
                .toList();
        return new AdminMarketSettlementDetailPageResponse(
                content,
                page,
                size,
                totalElements,
                totalPages(totalElements, size),
                isLast(page, size, totalElements)
        );
    }

    public AdminMarketRefundResponse getRefund(long marketId) {
        Market market = findMarket(marketId);
        MarketVoid marketVoid = marketMapper.selectMarketVoidByMarketIdForRead(marketId);
        if (marketVoid == null) {
            return emptyRefund(market);
        }
        AdminTransactionStatusCountsRow counts = marketMapper.selectRefundDetailStatusCounts(marketVoid.getId());
        boolean refundRequired = market.getStatus() == MarketStatus.VOIDED
                && ((counts.getTotalCount() > 0 && marketVoid.getRefundStatus() != RefundStatus.COMPLETED)
                || (counts.getTotalCount() == 0 && marketMapper.countConfirmedPredictionsForRefund(marketId) > 0));
        return new AdminMarketRefundResponse(
                market.getId(),
                market.getTitle(),
                market.getStatus(),
                marketVoid.getId(),
                marketVoid.getReasonType(),
                marketVoid.getReasonDetail(),
                marketVoid.getRefundStatus(),
                refundRequired,
                amount(counts.getTotalAmount()),
                counts.getTotalCount(),
                counts.getSuccessCount(),
                counts.getFailedCount(),
                counts.getUnknownCount(),
                counts.getPendingCount(),
                marketVoid.getCreatedAt(),
                marketVoid.getUpdatedAt()
        );
    }

    public AdminMarketRefundDetailPageResponse getRefundDetails(
            long marketId,
            long voidId,
            int page,
            int size,
            String statusValue
    ) {
        findMarket(marketId);
        MarketVoid marketVoid = marketMapper.selectMarketVoidByIdForRead(voidId);
        if (marketVoid == null || !Long.valueOf(marketId).equals(marketVoid.getMarketId())) {
            throw new CustomException(MarketErrorCode.MARKET_REFUND_NOT_ALLOWED);
        }
        TransactionItemStatus status = transactionStatus(statusValue);
        long totalElements = marketMapper.countRefundDetailsForAdmin(voidId, status);
        List<AdminRefundDetailResponse> content = marketMapper.selectRefundDetailsForAdmin(
                        voidId,
                        status,
                        page * size,
                        size
                ).stream()
                .map(this::toRefundDetail)
                .toList();
        return new AdminMarketRefundDetailPageResponse(
                content,
                page,
                size,
                totalElements,
                totalPages(totalElements, size),
                isLast(page, size, totalElements)
        );
    }

    public AdminMarketStatusCountsResponse getStatusCounts() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pendingThreshold = now.minusMinutes(STALE_MINUTES);
        AdminMarketStatusCountsRow counts = marketMapper.selectAdminMarketStatusCounts(now);
        return new AdminMarketStatusCountsResponse(
                counts.getTotal(),
                counts.getPending(),
                counts.getActive(),
                counts.getClosedByTime(),
                counts.getClosed(),
                counts.getDataPending(),
                counts.getSettlementInProgress(),
                counts.getSettled(),
                counts.getVoided(),
                marketMapper.countDistinctProblemMarketsForAdmin(pendingThreshold)
        );
    }

    private Market findMarket(long marketId) {
        Market market = marketMapper.selectAdminMarketById(marketId);
        if (market == null) {
            throw new CustomException(MarketErrorCode.MARKET_NOT_FOUND);
        }
        return market;
    }

    private AdminMarketDetailResponse.AdminMarketOption toOption(
            MarketOption option,
            BigDecimal totalVirtualPool
    ) {
        BigDecimal virtualPool = required(option.getVirtualPoolAmount());
        BigDecimal realPool = required(option.getRealPoolAmount());
        BigDecimal currentPrice = required(option.getCurrentPrice());
        BigDecimal initialPrice = virtualPool.divide(totalVirtualPool, RATE_SCALE, RoundingMode.HALF_UP);
        if (initialPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(MarketErrorCode.MARKET_INVALID_OPTION);
        }
        BigDecimal priceChangeRate = currentPrice.subtract(initialPrice)
                .divide(initialPrice, RATE_SCALE, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(RATE_SCALE, RoundingMode.HALF_UP);
        return new AdminMarketDetailResponse.AdminMarketOption(
                option.getId(),
                option.getOptionCode(),
                option.getOptionText(),
                option.getDisplayOrder(),
                option.getRangeMin(),
                option.getRangeMax(),
                option.getMinInclusive(),
                option.getMaxInclusive(),
                initialPrice,
                currentPrice,
                priceChangeRate,
                realPool,
                virtualPool,
                realPool.add(virtualPool),
                option.getTotalContractQuantity(),
                option.getPredictionCount(),
                option.getIsResult()
        );
    }

    private AdminMarketDetailResponse.AdminSettlementSummary settlementSummary(long marketId) {
        MarketSettlement settlement = marketMapper.selectLatestMarketSettlementByMarketIdForRead(marketId);
        if (settlement == null) {
            return new AdminMarketDetailResponse.AdminSettlementSummary(null, null, 0, 0, 0, 0, 0, null);
        }
        AdminTransactionStatusCountsRow counts = marketMapper.selectSettlementDetailStatusCounts(settlement.getId());
        return new AdminMarketDetailResponse.AdminSettlementSummary(
                settlement.getId(),
                settlement.getStatus(),
                counts.getTotalCount(),
                counts.getSuccessCount(),
                counts.getFailedCount(),
                counts.getUnknownCount(),
                counts.getPendingCount(),
                settlement.getUpdatedAt()
        );
    }

    private AdminMarketDetailResponse.AdminRefundSummary refundSummary(Market market) {
        MarketVoid marketVoid = marketMapper.selectMarketVoidByMarketIdForRead(market.getId());
        if (marketVoid == null) {
            return new AdminMarketDetailResponse.AdminRefundSummary(null, null, null, false, 0, 0, 0, 0, 0, null);
        }
        AdminTransactionStatusCountsRow counts = marketMapper.selectRefundDetailStatusCounts(marketVoid.getId());
        boolean refundRequired = market.getStatus() == MarketStatus.VOIDED
                && ((counts.getTotalCount() > 0 && marketVoid.getRefundStatus() != RefundStatus.COMPLETED)
                || (counts.getTotalCount() == 0
                && marketMapper.countConfirmedPredictionsForRefund(market.getId()) > 0));
        return new AdminMarketDetailResponse.AdminRefundSummary(
                marketVoid.getId(),
                marketVoid.getReasonType(),
                marketVoid.getRefundStatus(),
                refundRequired,
                counts.getTotalCount(),
                counts.getSuccessCount(),
                counts.getFailedCount(),
                counts.getUnknownCount(),
                counts.getPendingCount(),
                marketVoid.getUpdatedAt()
        );
    }

    private AdminMarketProblemPageResponse.Problem toProblem(AdminMarketProblemRow row) {
        boolean manualCheckRequired = row.getProblemType() == AdminMarketProblemType.REPUTATION
                && row.getFailedCount() > 0;
        return new AdminMarketProblemPageResponse.Problem(
                row.getMarketId(),
                row.getTitle(),
                row.getMarketStatus(),
                row.getProblemType(),
                problemStatus(row),
                row.getFailedCount(),
                row.getUnknownCount(),
                row.getPendingStaleCount(),
                row.getLastErrorCode(),
                row.getLastErrorMessage(),
                row.getLastAttemptAt(),
                !manualCheckRequired,
                manualCheckRequired
        );
    }

    private AdminMarketProblemStatus problemStatus(AdminMarketProblemRow row) {
        if (row.getFailedCount() > 0) {
            return AdminMarketProblemStatus.FAILED;
        }
        if (row.getUnknownCount() > 0) {
            return AdminMarketProblemStatus.UNKNOWN;
        }
        if (row.getPendingStaleCount() > 0) {
            return AdminMarketProblemStatus.PENDING_STALE;
        }
        return AdminMarketProblemStatus.NEEDS_CHECK;
    }

    private AdminSettlementDetailResponse toSettlementDetail(AdminSettlementDetailRow detail) {
        return new AdminSettlementDetailResponse(
                detail.getSettlementDetailId(),
                detail.getSettlementId(),
                detail.getPredictionId(),
                detail.getMemberId(),
                detail.getSelectedOptionId(),
                detail.getPointAmount(),
                detail.getContractQuantity(),
                detail.getSettledAmount(),
                detail.getProfitAmount(),
                detail.getStatus(),
                detail.getFailureReason(),
                detail.getIdempotencyKey(),
                detail.getCreatedAt(),
                detail.getUpdatedAt()
        );
    }

    private AdminRefundDetailResponse toRefundDetail(AdminRefundDetailRow detail) {
        return new AdminRefundDetailResponse(
                detail.getRefundDetailId(),
                detail.getVoidId(),
                detail.getPredictionId(),
                detail.getMemberId(),
                detail.getPointAmount(),
                detail.getRefundAmount(),
                detail.getStatus(),
                detail.getFailureReason(),
                detail.getIdempotencyKey(),
                detail.getCreatedAt(),
                detail.getUpdatedAt()
        );
    }

    private AdminMarketSettlementResponse emptySettlement(Market market) {
        return new AdminMarketSettlementResponse(
                market.getId(), market.getTitle(), market.getStatus(), null, null, null, null,
                ZERO_AMOUNT, null, ZERO_AMOUNT, ZERO_AMOUNT, ZERO_QUANTITY, ZERO_QUANTITY, ZERO_AMOUNT,
                0, 0, 0, 0, 0, null, null
        );
    }

    private AdminMarketRefundResponse emptyRefund(Market market) {
        return new AdminMarketRefundResponse(
                market.getId(), market.getTitle(), market.getStatus(), null, null, null, null, false,
                ZERO_AMOUNT, 0, 0, 0, 0, 0, null, null
        );
    }

    private TransactionItemStatus transactionStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return TransactionItemStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(CommonErrorCode.VALIDATION_FAILED);
        }
    }

    private boolean canPredict(Market market, LocalDateTime now) {
        return market.getStatus() == MarketStatus.ACTIVE && market.getCloseAt().isAfter(now);
    }

    private MarketDisplayStatus displayStatus(Market market, LocalDateTime now) {
        if (market.getStatus() == MarketStatus.ACTIVE && !market.getCloseAt().isAfter(now)) {
            return MarketDisplayStatus.CLOSED_BY_TIME;
        }
        return MarketDisplayStatus.valueOf(market.getStatus().name());
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? ZERO_AMOUNT : value;
    }

    private BigDecimal required(BigDecimal value) {
        if (value == null) {
            throw new CustomException(MarketErrorCode.MARKET_INVALID_OPTION);
        }
        return value;
    }

    private int totalPages(long totalElements, int size) {
        return totalElements == 0 ? 0 : (int) ((totalElements + size - 1) / size);
    }

    private boolean isLast(int page, int size, long totalElements) {
        return (long) (page + 1) * size >= totalElements;
    }
}

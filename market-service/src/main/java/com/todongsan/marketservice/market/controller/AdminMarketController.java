package com.todongsan.marketservice.market.controller;

import com.todongsan.marketservice.global.auth.AdminOnly;
import com.todongsan.marketservice.global.response.ApiResponse;
import com.todongsan.marketservice.market.dto.request.ConfirmMarketResultRequest;
import com.todongsan.marketservice.market.dto.request.CreateMarketRequest;
import com.todongsan.marketservice.market.dto.request.VoidMarketRequest;
import com.todongsan.marketservice.market.dto.response.ActivateMarketResponse;
import com.todongsan.marketservice.market.dto.response.AdminMarketDetailResponse;
import com.todongsan.marketservice.market.dto.response.AdminMarketProblemPageResponse;
import com.todongsan.marketservice.market.dto.response.AdminMarketRefundDetailPageResponse;
import com.todongsan.marketservice.market.dto.response.AdminMarketRefundResponse;
import com.todongsan.marketservice.market.dto.response.AdminMarketSettlementDetailPageResponse;
import com.todongsan.marketservice.market.dto.response.AdminMarketSettlementResponse;
import com.todongsan.marketservice.market.dto.response.AdminMarketStatusCountsResponse;
import com.todongsan.marketservice.market.dto.response.ConfirmMarketResultResponse;
import com.todongsan.marketservice.market.dto.response.CreateMarketResponse;
import com.todongsan.marketservice.market.dto.response.RefundMarketResponse;
import com.todongsan.marketservice.market.dto.response.SettleMarketResponse;
import com.todongsan.marketservice.market.dto.response.VoidMarketResponse;
import com.todongsan.marketservice.market.service.AdminMarketService;
import com.todongsan.marketservice.market.service.AdminMarketQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequiredArgsConstructor
@AdminOnly
@RequestMapping("/api/v1/admin/markets")
@Tag(name = "Market Admin API", description = "Market admin APIs. X-Member-Role=ADMIN is required.")
@io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_FAILED / MARKET_INVALID_OPTION / MARKET_INVALID_OPTION_RANGE"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FORBIDDEN"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MARKET_NOT_FOUND / MARKET_OPTION_NOT_FOUND"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "MARKET_INVALID_STATUS / MARKET_CLOSED / MARKET_NO_PREDICTIONS / MARKET_ALREADY_SETTLED / MARKET_REFUND_NOT_ALLOWED")
})
public class AdminMarketController {

    private static final String ADMIN_ROLE_HEADER_DESCRIPTION =
            "Member role injected by Gateway. Admin APIs require ADMIN. Market Service does not parse JWT.";

    private final AdminMarketService adminMarketService;
    private final AdminMarketQueryService adminMarketQueryService;

    @GetMapping("/problem-markets")
    @Operation(
            summary = "Get problem Markets",
            description = "Lists scheduler recovery targets and problems that require administrator observation.",
            parameters = @Parameter(name = "X-Member-Role", description = ADMIN_ROLE_HEADER_DESCRIPTION,
                    required = true, in = ParameterIn.HEADER, example = "ADMIN")
    )
    public ApiResponse<AdminMarketProblemPageResponse> getProblemMarkets(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "ALL") String type
    ) {
        return ApiResponse.ok(adminMarketQueryService.getProblemMarkets(page, size, type));
    }

    @GetMapping("/status-counts")
    @Operation(
            summary = "Get admin Market status counts",
            parameters = @Parameter(name = "X-Member-Role", description = ADMIN_ROLE_HEADER_DESCRIPTION,
                    required = true, in = ParameterIn.HEADER, example = "ADMIN")
    )
    public ApiResponse<AdminMarketStatusCountsResponse> getStatusCounts() {
        return ApiResponse.ok(adminMarketQueryService.getStatusCounts());
    }

    @GetMapping("/{marketId}")
    @Operation(
            summary = "Get admin Market detail",
            parameters = @Parameter(name = "X-Member-Role", description = ADMIN_ROLE_HEADER_DESCRIPTION,
                    required = true, in = ParameterIn.HEADER, example = "ADMIN")
    )
    public ApiResponse<AdminMarketDetailResponse> getMarket(
            @PathVariable @Min(1) long marketId
    ) {
        return ApiResponse.ok(adminMarketQueryService.getMarket(marketId));
    }

    @GetMapping("/{marketId}/settlements")
    @Operation(
            summary = "Get Market settlement summary",
            parameters = @Parameter(name = "X-Member-Role", description = ADMIN_ROLE_HEADER_DESCRIPTION,
                    required = true, in = ParameterIn.HEADER, example = "ADMIN")
    )
    public ApiResponse<AdminMarketSettlementResponse> getSettlement(
            @PathVariable @Min(1) long marketId
    ) {
        return ApiResponse.ok(adminMarketQueryService.getSettlement(marketId));
    }

    @GetMapping("/{marketId}/settlements/{settlementId}/details")
    @Operation(
            summary = "Get Market settlement details",
            parameters = @Parameter(name = "X-Member-Role", description = ADMIN_ROLE_HEADER_DESCRIPTION,
                    required = true, in = ParameterIn.HEADER, example = "ADMIN")
    )
    public ApiResponse<AdminMarketSettlementDetailPageResponse> getSettlementDetails(
            @PathVariable @Min(1) long marketId,
            @PathVariable @Min(1) long settlementId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.ok(adminMarketQueryService.getSettlementDetails(
                marketId, settlementId, page, size, status
        ));
    }

    @GetMapping("/{marketId}/refunds")
    @Operation(
            summary = "Get Market refund summary",
            parameters = @Parameter(name = "X-Member-Role", description = ADMIN_ROLE_HEADER_DESCRIPTION,
                    required = true, in = ParameterIn.HEADER, example = "ADMIN")
    )
    public ApiResponse<AdminMarketRefundResponse> getRefund(
            @PathVariable @Min(1) long marketId
    ) {
        return ApiResponse.ok(adminMarketQueryService.getRefund(marketId));
    }

    @GetMapping("/{marketId}/refunds/{voidId}/details")
    @Operation(
            summary = "Get Market refund details",
            parameters = @Parameter(name = "X-Member-Role", description = ADMIN_ROLE_HEADER_DESCRIPTION,
                    required = true, in = ParameterIn.HEADER, example = "ADMIN")
    )
    public ApiResponse<AdminMarketRefundDetailPageResponse> getRefundDetails(
            @PathVariable @Min(1) long marketId,
            @PathVariable @Min(1) long voidId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.ok(adminMarketQueryService.getRefundDetails(marketId, voidId, page, size, status));
    }

    @PostMapping
    @Operation(
            summary = "Create admin Market",
            description = "Creates a Market in PENDING status. This API does not activate the Market, allow predictions, or create PriceHistory.",
            parameters = @Parameter(
                    name = "X-Member-Role",
                    description = ADMIN_ROLE_HEADER_DESCRIPTION,
                    required = true,
                    in = ParameterIn.HEADER,
                    example = "ADMIN"
            )
    )
    public ApiResponse<CreateMarketResponse> createMarket(
            @Valid @RequestBody CreateMarketRequest request
    ) {
        return ApiResponse.ok(adminMarketService.createMarket(request));
    }

    @PatchMapping("/{marketId}/activate")
    @Operation(
            summary = "Activate admin Market",
            description = "Changes a PENDING Market to ACTIVE.",
            parameters = @Parameter(
                    name = "X-Member-Role",
                    description = ADMIN_ROLE_HEADER_DESCRIPTION,
                    required = true,
                    in = ParameterIn.HEADER,
                    example = "ADMIN"
            )
    )
    public ApiResponse<ActivateMarketResponse> activateMarket(
            @Parameter(description = "Market ID", example = "1")
            @PathVariable long marketId
    ) {
        return ApiResponse.ok(adminMarketService.activateMarket(marketId));
    }

    @PatchMapping("/{marketId}/result")
    @Operation(
            summary = "Confirm Market result",
            description = "Confirms the result option and changes the Market to CLOSED. Fails when POINT_PENDING or POINT_UNKNOWN predictions remain.",
            parameters = @Parameter(
                    name = "X-Member-Role",
                    description = ADMIN_ROLE_HEADER_DESCRIPTION,
                    required = true,
                    in = ParameterIn.HEADER,
                    example = "ADMIN"
            )
    )
    public ApiResponse<ConfirmMarketResultResponse> confirmMarketResult(
            @Parameter(description = "Market ID", example = "1")
            @PathVariable long marketId,
            @Valid @RequestBody ConfirmMarketResultRequest request
    ) {
        return ApiResponse.ok(adminMarketService.confirmMarketResult(marketId, request));
    }

    @PostMapping("/{marketId}/settlements")
    @Operation(
            summary = "Start Market settlement",
            description = "Starts settlement for a CLOSED Market. Uses Member-Point batch integration and item idempotencyKey policy.",
            parameters = @Parameter(
                    name = "X-Member-Role",
                    description = ADMIN_ROLE_HEADER_DESCRIPTION,
                    required = true,
                    in = ParameterIn.HEADER,
                    example = "ADMIN"
            )
    )
    public ApiResponse<SettleMarketResponse> settleMarket(
            @Parameter(description = "Market ID", example = "1")
            @PathVariable long marketId
    ) {
        return ApiResponse.ok(adminMarketService.settleMarket(marketId));
    }

    @PostMapping("/{marketId}/settlements/retry")
    @Operation(
            summary = "Retry Market settlement",
            description = "Retries failed or unknown settlement items.",
            parameters = @Parameter(
                    name = "X-Member-Role",
                    description = ADMIN_ROLE_HEADER_DESCRIPTION,
                    required = true,
                    in = ParameterIn.HEADER,
                    example = "ADMIN"
            )
    )
    public ApiResponse<SettleMarketResponse> retryMarketSettlement(
            @Parameter(description = "Market ID", example = "1")
            @PathVariable long marketId
    ) {
        return ApiResponse.ok(adminMarketService.retryMarketSettlement(marketId));
    }

    @PostMapping("/{marketId}/refunds")
    @Operation(
            summary = "Start Market refund",
            description = "Starts refunds for a voided Market. Uses Member-Point batch integration and item idempotencyKey policy.",
            parameters = @Parameter(
                    name = "X-Member-Role",
                    description = ADMIN_ROLE_HEADER_DESCRIPTION,
                    required = true,
                    in = ParameterIn.HEADER,
                    example = "ADMIN"
            )
    )
    public ApiResponse<RefundMarketResponse> refundMarket(
            @Parameter(description = "Market ID", example = "1")
            @PathVariable long marketId
    ) {
        return ApiResponse.ok(adminMarketService.refundMarket(marketId));
    }

    @PostMapping("/{marketId}/refunds/retry")
    @Operation(
            summary = "Retry Market refund",
            description = "Retries failed or unknown refund items.",
            parameters = @Parameter(
                    name = "X-Member-Role",
                    description = ADMIN_ROLE_HEADER_DESCRIPTION,
                    required = true,
                    in = ParameterIn.HEADER,
                    example = "ADMIN"
            )
    )
    public ApiResponse<RefundMarketResponse> retryRefundMarket(
            @Parameter(description = "Market ID", example = "1")
            @PathVariable long marketId
    ) {
        return ApiResponse.ok(adminMarketService.retryRefundMarket(marketId));
    }

    @PatchMapping("/{marketId}/void")
    @Operation(
            summary = "Void Market",
            description = "Changes an allowed Market to VOIDED and prepares refund processing.",
            parameters = @Parameter(
                    name = "X-Member-Role",
                    description = ADMIN_ROLE_HEADER_DESCRIPTION,
                    required = true,
                    in = ParameterIn.HEADER,
                    example = "ADMIN"
            )
    )
    public ApiResponse<VoidMarketResponse> voidMarket(
            @Parameter(description = "Market ID", example = "1")
            @PathVariable long marketId,
            @Valid @RequestBody VoidMarketRequest request
    ) {
        return ApiResponse.ok(adminMarketService.voidMarket(marketId, request));
    }
}

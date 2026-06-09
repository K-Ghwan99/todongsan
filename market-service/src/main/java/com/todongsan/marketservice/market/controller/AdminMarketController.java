package com.todongsan.marketservice.market.controller;

import com.todongsan.marketservice.global.auth.AdminOnly;
import com.todongsan.marketservice.global.response.ApiResponse;
import com.todongsan.marketservice.market.dto.request.ConfirmMarketResultRequest;
import com.todongsan.marketservice.market.dto.request.CreateMarketRequest;
import com.todongsan.marketservice.market.dto.request.VoidMarketRequest;
import com.todongsan.marketservice.market.dto.response.ActivateMarketResponse;
import com.todongsan.marketservice.market.dto.response.ConfirmMarketResultResponse;
import com.todongsan.marketservice.market.dto.response.CreateMarketResponse;
import com.todongsan.marketservice.market.dto.response.RefundMarketResponse;
import com.todongsan.marketservice.market.dto.response.SettleMarketResponse;
import com.todongsan.marketservice.market.dto.response.VoidMarketResponse;
import com.todongsan.marketservice.market.service.AdminMarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

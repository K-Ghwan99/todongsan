package com.todongsan.marketservice.market.controller;

import com.todongsan.marketservice.global.response.ApiResponse;
import com.todongsan.marketservice.market.dto.request.ConfirmMarketResultRequest;
import com.todongsan.marketservice.market.dto.request.CreateMarketRequest;
import com.todongsan.marketservice.market.dto.response.ActivateMarketResponse;
import com.todongsan.marketservice.market.dto.response.ConfirmMarketResultResponse;
import com.todongsan.marketservice.market.dto.response.CreateMarketResponse;
import com.todongsan.marketservice.market.service.AdminMarketService;
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
@RequestMapping("/api/v1/admin/markets")
public class AdminMarketController {

    private final AdminMarketService adminMarketService;

    @PostMapping
    public ApiResponse<CreateMarketResponse> createMarket(
            @Valid @RequestBody CreateMarketRequest request
    ) {
        return ApiResponse.ok(adminMarketService.createMarket(request));
    }

    @PatchMapping("/{marketId}/activate")
    public ApiResponse<ActivateMarketResponse> activateMarket(
            @PathVariable long marketId
    ) {
        return ApiResponse.ok(adminMarketService.activateMarket(marketId));
    }

    @PatchMapping("/{marketId}/result")
    public ApiResponse<ConfirmMarketResultResponse> confirmMarketResult(
            @PathVariable long marketId,
            @Valid @RequestBody ConfirmMarketResultRequest request
    ) {
        return ApiResponse.ok(adminMarketService.confirmMarketResult(marketId, request));
    }
}

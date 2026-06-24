package com.todongsan.insightreputation.insight.controller;

import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import com.todongsan.insightreputation.global.response.ApiResponse;
import com.todongsan.insightreputation.insight.dto.ActivityTrendResponse;
import com.todongsan.insightreputation.insight.dto.PlatformOverviewResponse;
import com.todongsan.insightreputation.insight.dto.RegionPriceMapResponse;
import com.todongsan.insightreputation.insight.service.AdminPlatformInsightService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/insights")
@RequiredArgsConstructor
@Validated
public class AdminPlatformInsightController {

    private final AdminPlatformInsightService adminPlatformInsightService;

    @GetMapping("/overview")
    public ApiResponse<PlatformOverviewResponse> getOverview(
            @RequestHeader(value = "X-Member-Role", required = false) String role) {
        if (!"ADMIN".equals(role)) throw new CustomException(ErrorCode.FORBIDDEN);
        return ApiResponse.success(adminPlatformInsightService.getOverview());
    }

    @GetMapping("/regions/price-map")
    public ApiResponse<RegionPriceMapResponse> getRegionPriceMap(
            @RequestHeader(value = "X-Member-Role", required = false) String role) {
        if (!"ADMIN".equals(role)) throw new CustomException(ErrorCode.FORBIDDEN);
        return ApiResponse.success(adminPlatformInsightService.getRegionPriceMap());
    }

    @GetMapping("/activity/trend")
    public ApiResponse<ActivityTrendResponse> getActivityTrend(
            @RequestHeader(value = "X-Member-Role", required = false) String role,
            @RequestParam(defaultValue = "12") @Min(1) @Max(52) int weeks) {
        if (!"ADMIN".equals(role)) throw new CustomException(ErrorCode.FORBIDDEN);
        return ApiResponse.success(adminPlatformInsightService.getActivityTrend(weeks));
    }
}

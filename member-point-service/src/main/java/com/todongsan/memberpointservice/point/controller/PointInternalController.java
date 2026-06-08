package com.todongsan.memberpointservice.point.controller;

import com.todongsan.memberpointservice.global.response.ApiResponse;
import com.todongsan.memberpointservice.point.dto.request.EarnRequest;
import com.todongsan.memberpointservice.point.dto.request.RefundRequest;
import com.todongsan.memberpointservice.point.dto.request.SettlementRequest;
import com.todongsan.memberpointservice.point.dto.request.SpendRequest;
import com.todongsan.memberpointservice.point.dto.response.EarnResponse;
import com.todongsan.memberpointservice.point.dto.response.RefundResponse;
import com.todongsan.memberpointservice.point.dto.response.SettlementResponse;
import com.todongsan.memberpointservice.point.dto.response.SpendResponse;
import com.todongsan.memberpointservice.point.service.PointInternalService;
import com.todongsan.memberpointservice.point.service.PointResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/api/v1/points")
@RequiredArgsConstructor
public class PointInternalController {

    private final PointInternalService pointInternalService;

    @PostMapping("/earn")
    public ResponseEntity<ApiResponse<EarnResponse>> earn(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody EarnRequest request) {
        PointResult<EarnResponse> result = pointInternalService.earn(idempotencyKey, request);
        if (result.alreadyProcessed()) {
            return ResponseEntity.ok(ApiResponse.alreadyProcessed(result.data()));
        }
        return ResponseEntity.ok(ApiResponse.ok(result.data()));
    }

    @PostMapping("/spend")
    public ResponseEntity<ApiResponse<SpendResponse>> spend(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody SpendRequest request) {
        PointResult<SpendResponse> result = pointInternalService.spend(idempotencyKey, request);
        if (result.alreadyProcessed()) {
            return ResponseEntity.ok(ApiResponse.alreadyProcessed(result.data()));
        }
        return ResponseEntity.ok(ApiResponse.ok(result.data()));
    }

    @PostMapping("/settlements")
    public ResponseEntity<ApiResponse<SettlementResponse>> settle(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody SettlementRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(pointInternalService.settle(idempotencyKey, request)));
    }

    @PostMapping("/refunds")
    public ResponseEntity<ApiResponse<RefundResponse>> refund(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody RefundRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(pointInternalService.refund(idempotencyKey, request)));
    }

}

package com.todongsan.memberpointservice.point.service;

import com.todongsan.memberpointservice.point.dto.request.EarnRequest;
import com.todongsan.memberpointservice.point.dto.request.RefundRequest;
import com.todongsan.memberpointservice.point.dto.request.SettlementRequest;
import com.todongsan.memberpointservice.point.dto.request.SpendRequest;
import com.todongsan.memberpointservice.point.dto.response.EarnResponse;
import com.todongsan.memberpointservice.point.dto.response.RefundResponse;
import com.todongsan.memberpointservice.point.dto.response.SettlementResponse;
import com.todongsan.memberpointservice.point.dto.response.SpendResponse;

public interface PointInternalService {

    PointResult<EarnResponse> earn(String idempotencyKey, EarnRequest request);

    PointResult<SpendResponse> spend(String idempotencyKey, SpendRequest request);

    SettlementResponse settle(String idempotencyKey, SettlementRequest request);

    RefundResponse refund(String idempotencyKey, RefundRequest request);

}

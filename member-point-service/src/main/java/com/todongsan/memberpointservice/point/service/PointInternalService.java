package com.todongsan.memberpointservice.point.service;

import com.todongsan.memberpointservice.point.dto.request.EarnRequest;
import com.todongsan.memberpointservice.point.dto.response.EarnResponse;

public interface PointInternalService {

    PointResult<EarnResponse> earn(String idempotencyKey, EarnRequest request);
}

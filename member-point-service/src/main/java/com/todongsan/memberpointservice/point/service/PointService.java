package com.todongsan.memberpointservice.point.service;

import com.todongsan.memberpointservice.point.dto.response.PointBalanceResponse;
import com.todongsan.memberpointservice.point.dto.response.PointHistoryPageResponse;

public interface PointService {

    PointBalanceResponse getBalance(Long memberId);

    PointHistoryPageResponse getHistory(Long memberId, String type, int page, int size);

}

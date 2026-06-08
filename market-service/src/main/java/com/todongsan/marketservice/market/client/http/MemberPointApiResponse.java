package com.todongsan.marketservice.market.client.http;

import java.time.LocalDateTime;

public record MemberPointApiResponse<T>(
        Boolean success,
        String errorCode,
        String message,
        T data,
        LocalDateTime timestamp
) {
}

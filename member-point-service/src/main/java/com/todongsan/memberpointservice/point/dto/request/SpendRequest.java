package com.todongsan.memberpointservice.point.dto.request;

import lombok.Getter;
import java.math.BigDecimal;

@Getter
public class SpendRequest {
    private Long memberId;
    private String type;
    private BigDecimal amount;
    private String referenceType;
    private Long referenceId;
    private String reason;
}

package com.todongsan.memberpointservice.point.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse {

    private String idempotencyKey;
    private String status;          // PROCESSED | FAILED | NOT_FOUND
    private Long memberId;
    private String type;
    private String amount;
    private String referenceType;
    private Long referenceId;
    private String balanceSnapshot;
    private LocalDateTime createdAt;
    private String failReason;

}

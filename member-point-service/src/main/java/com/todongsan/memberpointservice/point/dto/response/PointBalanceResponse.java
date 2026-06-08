package com.todongsan.memberpointservice.point.dto.response;

import com.todongsan.memberpointservice.member.entity.Member;
import lombok.Getter;

@Getter
public class PointBalanceResponse {

    private final Long memberId;
    private final String pointBalance;

    public PointBalanceResponse(Member member) {
        this.memberId = member.getId();
        this.pointBalance = member.getPointBalance().toPlainString();
    }
}

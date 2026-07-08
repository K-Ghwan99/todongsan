package com.todongsan.memberpointservice.member.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.todongsan.memberpointservice.member.entity.Member;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MemberResponse {

    private final Long memberId;
    private final String nickname;
    private final String email;
    private final String role;
    private final String residenceSido;
    private final String residenceSigu;
    private final String pointBalance;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime createdAt;

    public MemberResponse(Member member) {
        this.memberId = member.getId();
        this.nickname = member.getNickname();
        this.email = member.getEmail();
        this.role = member.getRole().name();
        this.residenceSido = member.getResidenceSido();
        this.residenceSigu = member.getResidenceSigu();
        // BigDecimal → 소수점 2자리 고정 문자열
        this.pointBalance = member.getPointBalance().toPlainString();
        this.createdAt = member.getCreatedAt();
    }
}

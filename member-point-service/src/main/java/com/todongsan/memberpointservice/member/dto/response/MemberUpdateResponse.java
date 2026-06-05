package com.todongsan.memberpointservice.member.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.todongsan.memberpointservice.member.entity.Member;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MemberUpdateResponse {

    private final Long memberId;
    private final String nickname;
    private final String residenceSido;
    private final String residenceSigu;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime residenceChangedAt;

    public MemberUpdateResponse(Member member) {
        this.memberId = member.getId();
        this.nickname = member.getNickname();
        this.residenceSido = member.getResidenceSido();
        this.residenceSigu = member.getResidenceSigu();
        this.residenceChangedAt = member.getResidenceChangedAt();
    }

}

package com.todongsan.memberpointservice.member.dto.response;

import com.todongsan.memberpointservice.member.entity.Member;
import lombok.Getter;

@Getter
public class MemberBatchItemResponse {

    private final Long memberId;
    private final String ageGroup;
    private final String gender;
    private final String residenceSido;
    private final String residenceSigu;

    public MemberBatchItemResponse(Member member) {
        this.memberId = member.getId();
        this.ageGroup = member.getAgeGroup() != null ? member.getAgeGroup().getLabel() : "UNKNOWN";
        this.gender = member.getGender() != null ? member.getGender().name() : "UNKNOWN";
        this.residenceSido = member.getResidenceSido();
        this.residenceSigu = member.getResidenceSigu();
    }

}

package com.todongsan.insightreputation.global.client;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberInfoResponse {
    
    private Long memberId;
    private String ageGroup;        // "20대", "30대", "40대", "50대 이상"
    private String gender;          // "MALE", "FEMALE"
    private String residenceSido;
    private String residenceSigu;
}
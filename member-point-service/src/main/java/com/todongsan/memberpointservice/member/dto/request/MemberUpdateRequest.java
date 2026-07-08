package com.todongsan.memberpointservice.member.dto.request;

import lombok.Getter;

@Getter
public class MemberUpdateRequest {

    // 변경할 닉네임(null이면 변경 안 함)
    private String nickname;

    // 변경할 거주지 시/도 (null이면 변경 안 함)
    private String residenceSido;

    // 변경할 거주지 시/구 (null이면 변경 안 함)
    private String residenceSigu;

}

package com.todongsan.memberpointservice.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 카카오 로그인 요청
@Getter
@NoArgsConstructor
public class KakaoLoginRequest {

    @NotBlank(message = "카카오 액세스 토큰은 필수입니다.")
    private String accessToken;
}

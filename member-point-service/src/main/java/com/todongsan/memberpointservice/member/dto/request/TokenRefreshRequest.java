package com.todongsan.memberpointservice.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenRefreshRequest {

    @NotBlank(message = "refreshToken은 필수입니다.")
    private String refreshToken;
}

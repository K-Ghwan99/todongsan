package com.todongsan.memberpointservice.member.service;

import com.todongsan.memberpointservice.global.exception.CustomException;
import com.todongsan.memberpointservice.global.exception.ErrorCode;
import com.todongsan.memberpointservice.member.dto.KakaoUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

// 카카오 API 호출 전담
@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

    private final WebClient kakaoWebClient;

    // 카카오 사용자 정보 조회
    public KakaoUserInfo getUserInfo(String accessToken) {
        return kakaoWebClient.get()
                .uri("/v2/user/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(
                        status -> status.value() == 401,
                        response -> Mono.error(new CustomException(ErrorCode.KAKAO_AUTH_FAILED))
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> Mono.error(new CustomException(ErrorCode.KAKAO_SERVER_ERROR))
                )
                .bodyToMono(KakaoUserInfo.class)
                .block();
    }
}

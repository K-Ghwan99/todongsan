package com.todongsan.memberpointservice.member.service;

import com.todongsan.memberpointservice.global.exception.CustomException;
import com.todongsan.memberpointservice.global.exception.ErrorCode;
import com.todongsan.memberpointservice.member.dto.KakaoUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

// 카카오 API 호출 전담
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

    private final WebClient kakaoWebClient;

    // 카카오 사용자 정보 조회
    public KakaoUserInfo getUserInfo(String accessToken) {
        String tokenPreview = accessToken != null && accessToken.length() > 10
                ? accessToken.substring(0, 10) + "..."
                : accessToken;
        log.error("카카오 API 호출 시작. token: {}", tokenPreview);

        return kakaoWebClient.get()
                .uri("/v2/user/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(
                        status -> status.value() == 401,
                        response -> response.bodyToMono(String.class).defaultIfEmpty("(empty)").flatMap(body -> {
                            log.error("카카오 API 401 응답. body: {}", body);
                            return Mono.error(new CustomException(ErrorCode.KAKAO_AUTH_FAILED));
                        })
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> response.bodyToMono(String.class).defaultIfEmpty("(empty)").flatMap(body -> {
                            log.error("카카오 API 5xx 응답. status: {}, body: {}", response.statusCode(), body);
                            return Mono.error(new CustomException(ErrorCode.KAKAO_SERVER_ERROR));
                        })
                )
                .bodyToMono(KakaoUserInfo.class)
                .doOnError(e -> log.error("카카오 API 호출 실패. error: {}", e.getMessage(), e))
                .block();
    }
}

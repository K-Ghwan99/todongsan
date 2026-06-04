package com.todongsan.memberpointservice.member.service;

import com.todongsan.memberpointservice.member.dto.KakaoUserInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 카카오 API를 호출하는 통합 테스트.
 * 환경변수 KAKAO_ACCESS_TOKEN 세팅 필요.
 *
 * 실행: KAKAO_ACCESS_TOKEN=<토큰> ./gradlew test -Ptags=integration
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "KAKAO_ACCESS_TOKEN", matches = ".+")
class KakaoOAuthServiceIntegrationTest {

    @Test
    void 실제_카카오_API_사용자_정보_조회() {
        String accessToken = System.getenv("KAKAO_ACCESS_TOKEN");

        WebClient webClient = WebClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .build();
        KakaoOAuthService service = new KakaoOAuthService(webClient);

        KakaoUserInfo info = service.getUserInfo(accessToken);

        assertThat(info.getKakaoId()).isNotBlank();
        assertThat(info.getNickname()).isNotNull();
    }
}

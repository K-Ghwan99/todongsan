package com.todongsan.memberpointservice.member.service;

import com.todongsan.memberpointservice.global.exception.CustomException;
import com.todongsan.memberpointservice.global.exception.ErrorCode;
import com.todongsan.memberpointservice.member.dto.KakaoUserInfo;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KakaoOAuthServiceTest {

    private MockWebServer mockWebServer;
    private KakaoOAuthService kakaoOAuthService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();
        kakaoOAuthService = new KakaoOAuthService(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getUserInfo_성공_사용자_정보_파싱() throws InterruptedException {
        String body = """
                {
                  "id": 12345,
                  "kakao_account": {
                    "profile": { "nickname": "테스트유저" },
                    "email": "test@kakao.com",
                    "age_range": "20~29",
                    "gender": "male"
                  }
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body));

        KakaoUserInfo info = kakaoOAuthService.getUserInfo("test-access-token");

        assertThat(info.getKakaoId()).isEqualTo("12345");
        assertThat(info.getNickname()).isEqualTo("테스트유저");
        assertThat(info.getEmail()).isEqualTo("test@kakao.com");
        assertThat(info.getAgeRange()).isEqualTo("20~29");
        assertThat(info.getGender()).isEqualTo("male");

        // Authorization 헤더와 경로 검증
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-access-token");
        assertThat(request.getPath()).isEqualTo("/v2/user/me");
    }

    @Test
    void getUserInfo_401_KAKAO_AUTH_FAILED_예외() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));

        assertThatThrownBy(() -> kakaoOAuthService.getUserInfo("invalid-token"))
                .isInstanceOf(CustomException.class)
                .satisfies(ex ->
                        assertThat(((CustomException) ex).getErrorCode())
                                .isEqualTo(ErrorCode.KAKAO_AUTH_FAILED));
    }

    @Test
    void getUserInfo_500_KAKAO_SERVER_ERROR_예외() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> kakaoOAuthService.getUserInfo("test-token"))
                .isInstanceOf(CustomException.class)
                .satisfies(ex ->
                        assertThat(((CustomException) ex).getErrorCode())
                                .isEqualTo(ErrorCode.KAKAO_SERVER_ERROR));
    }
}

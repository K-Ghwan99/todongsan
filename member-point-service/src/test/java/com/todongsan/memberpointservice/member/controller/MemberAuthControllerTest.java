package com.todongsan.memberpointservice.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todongsan.memberpointservice.global.exception.GlobalExceptionHandler;
import com.todongsan.memberpointservice.member.dto.response.LoginResponse;
import com.todongsan.memberpointservice.member.service.MemberAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MemberAuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    MemberAuthService memberAuthService;

    @InjectMocks
    MemberAuthController memberAuthController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(memberAuthController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void kakaoLogin_성공_200() throws Exception {
        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken("access-jwt")
                .refreshToken("refresh-jwt")
                .memberId(1L)
                .nickname("테스트유저")
                .isNewMember(true)
                .build();
        when(memberAuthService.kakaoLogin(anyString())).thenReturn(loginResponse);

        String body = objectMapper.writeValueAsString(Map.of("accessToken", "kakao-access-token"));

        mockMvc.perform(post("/api/v1/members/oauth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-jwt"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-jwt"))
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andExpect(jsonPath("$.data.nickname").value("테스트유저"))
                .andExpect(jsonPath("$.data.newMember").value(true));
    }

    @Test
    void kakaoLogin_accessToken_누락_400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("accessToken", ""));

        mockMvc.perform(post("/api/v1/members/oauth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void kakaoLogin_바디_없음_400() throws Exception {
        mockMvc.perform(post("/api/v1/members/oauth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}

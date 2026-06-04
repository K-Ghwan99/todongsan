package com.todongsan.memberpointservice.global.security;

import com.todongsan.memberpointservice.global.exception.CustomException;
import com.todongsan.memberpointservice.global.exception.ErrorCode;
import com.todongsan.memberpointservice.member.entity.MemberRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private JwtProviderImpl jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProviderImpl();
        ReflectionTestUtils.setField(jwtProvider, "secret", "test-jwt-secret-must-be-32chars!");
        ReflectionTestUtils.setField(jwtProvider, "accessTokenExpiry", 21600000L);
        ReflectionTestUtils.setField(jwtProvider, "refreshTokenExpiry", 5184000000L);
    }

    @Test
    void generateAccessToken_memberId_role_라운드트립() {
        String token = jwtProvider.generateAccessToken(1L, MemberRole.USER);

        assertThat(jwtProvider.extractMemberId(token)).isEqualTo(1L);
        assertThat(jwtProvider.extractRole(token)).isEqualTo(MemberRole.USER);
    }

    @Test
    void generateRefreshToken_memberId_라운드트립() {
        String token = jwtProvider.generateRefreshToken(42L);

        assertThat(jwtProvider.extractMemberId(token)).isEqualTo(42L);
    }

    @Test
    void validateToken_유효한_토큰_예외없음() {
        String token = jwtProvider.generateAccessToken(1L, MemberRole.USER);

        // 예외 없이 통과해야 함
        jwtProvider.validateToken(token);
    }

    @Test
    void validateToken_만료된_토큰_TOKEN_EXPIRED() {
        // 만료 시간 0ms → 즉시 만료
        ReflectionTestUtils.setField(jwtProvider, "accessTokenExpiry", 0L);
        String token = jwtProvider.generateAccessToken(1L, MemberRole.USER);

        assertThatThrownBy(() -> jwtProvider.validateToken(token))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.TOKEN_EXPIRED));
    }

    @Test
    void validateToken_위조된_토큰_INVALID_TOKEN() {
        String token = jwtProvider.generateAccessToken(1L, MemberRole.USER);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtProvider.validateToken(tampered))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_TOKEN));
    }
}

package com.todongsan.memberpointservice.member.service;

import com.todongsan.memberpointservice.global.exception.CustomException;
import com.todongsan.memberpointservice.global.exception.ErrorCode;
import com.todongsan.memberpointservice.global.security.JwtProvider;
import com.todongsan.memberpointservice.global.util.CryptoUtil;
import com.todongsan.memberpointservice.member.dto.KakaoUserInfo;
import com.todongsan.memberpointservice.member.dto.response.LoginResponse;
import com.todongsan.memberpointservice.member.dto.response.TokenResponse;
import com.todongsan.memberpointservice.member.entity.*;
import com.todongsan.memberpointservice.member.repository.MemberRepository;
import com.todongsan.memberpointservice.member.repository.OauthTokenRepository;
import com.todongsan.memberpointservice.point.entity.PointHistory;
import com.todongsan.memberpointservice.point.entity.PointHistoryType;
import com.todongsan.memberpointservice.point.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

// 카카오 로그인 흐름 오케스트레이션
@Service
@RequiredArgsConstructor
public class MemberAuthServiceImpl implements MemberAuthService {

    private final KakaoOAuthService kakaoOAuthService;
    private final MemberRepository memberRepository;
    private final OauthTokenRepository oauthTokenRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final CryptoUtil cryptoUtil;
    private final JwtProvider jwtProvider;

    // 카카오 로그인 (신규/기존 회원 분기)
    @Override
    @Transactional
    public LoginResponse kakaoLogin(String accessToken) {
        KakaoUserInfo kakaoUserInfo = kakaoOAuthService.getUserInfo(accessToken);

        AgeGroup ageGroup = resolveAgeGroup(kakaoUserInfo.getAgeRange());
        Gender gender = resolveGender(kakaoUserInfo.getGender());

        boolean isNewMember;
        Member member;

        Optional<Member> existing = memberRepository.findByOauthProviderAndOauthId(
                "KAKAO", kakaoUserInfo.getKakaoId());

        if (existing.isEmpty()) {
            // 신규 회원
            member = memberRepository.save(Member.builder()
                    .nickname(kakaoUserInfo.getNickname())
                    .email(kakaoUserInfo.getEmail())
                    .oauthProvider("KAKAO")
                    .oauthId(kakaoUserInfo.getKakaoId())
                    .ageGroup(ageGroup)
                    .gender(gender)
                    .build());

            oauthTokenRepository.save(OauthToken.builder()
                    .member(member)
                    .provider("KAKAO")
                    .accessToken(cryptoUtil.encrypt(accessToken))
                    .refreshToken(cryptoUtil.encrypt(accessToken)) // TODO: 카카오 refreshToken 별도 수신 필요
                    .accessTokenExpiresAt(LocalDateTime.now().plusHours(6))
                    .refreshTokenExpiresAt(LocalDateTime.now().plusDays(60))
                    .build());

            // 가입 보상 200P (멱등성 보장)
            String signupKey = "SIGNUP:" + member.getId();
            if (pointHistoryRepository.findByIdempotencyKey(signupKey).isEmpty()) {
                memberRepository.earnPoint(member.getId(), BigDecimal.valueOf(200));
                Member afterEarn = memberRepository.findById(member.getId()).orElseThrow();

                pointHistoryRepository.save(PointHistory.builder()
                        .memberId(member.getId())
                        .type(PointHistoryType.EARN_SIGNUP)
                        .amount(BigDecimal.valueOf(200))
                        .balanceSnapshot(afterEarn.getPointBalance())
                        .reason("신규 가입 보상")
                        .idempotencyKey(signupKey)
                        .build());
            }

            isNewMember = true;
        } else {
            // 기존 회원 — 카카오 액세스 토큰만 갱신
            member = existing.get();

            OauthToken oauthToken = oauthTokenRepository.findByMemberId(member.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            oauthToken.updateTokens(
                    cryptoUtil.encrypt(accessToken),
                    oauthToken.getRefreshToken(),
                    LocalDateTime.now().plusHours(6),
                    oauthToken.getRefreshTokenExpiresAt()
            );

            isNewMember = false;
        }

        return LoginResponse.builder()
                .accessToken(jwtProvider.generateAccessToken(member.getId(), member.getRole()))
                .refreshToken(jwtProvider.generateRefreshToken(member.getId()))
                .memberId(member.getId())
                .nickname(member.getNickname())
                .isNewMember(isNewMember)
                .build();
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        jwtProvider.validateToken(refreshToken);
        Long memberId = jwtProvider.extractMemberId(refreshToken);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        return TokenResponse.builder()
                .accessToken(jwtProvider.generateAccessToken(member.getId(), member.getRole()))
                .refreshToken(jwtProvider.generateRefreshToken(member.getId()))
                .build();
    }

    // age_range → AgeGroup 변환 (카카오 응답: "20~29", "30~39" 등)
    private AgeGroup resolveAgeGroup(String ageRange) {
        if (ageRange == null) return AgeGroup.UNKNOWN;
        int start = Integer.parseInt(ageRange.split("~")[0]);
        if (start < 20) return AgeGroup.AGE_10S;
        if (start < 30) return AgeGroup.AGE_20S;
        if (start < 40) return AgeGroup.AGE_30S;
        if (start < 50) return AgeGroup.AGE_40S;
        return AgeGroup.AGE_50S_ABOVE;
    }

    // kakao gender 문자열 → Gender Enum 변환
    private Gender resolveGender(String gender) {
        if ("male".equals(gender)) return Gender.MALE;
        if ("female".equals(gender)) return Gender.FEMALE;
        return Gender.UNKNOWN;
    }
}

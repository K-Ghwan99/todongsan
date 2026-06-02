package com.todongsan.memberpointservice.member.entity;

import com.todongsan.memberpointservice.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 카카오 OAuth 토큰 (AES 암호화 저장)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "oauth_token")
public class OauthToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 회원 1:1 연관
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(nullable = false, length = 20)
    private String provider;

    // 액세스 토큰 (AES 암호화)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    // 리프레시 토큰 (AES 암호화)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String refreshToken;

    // 액세스 토큰 만료 시각
    @Column(nullable = false)
    private LocalDateTime accessTokenExpiresAt;

    // 리프레시 토큰 만료 시각
    @Column(nullable = false)
    private LocalDateTime refreshTokenExpiresAt;

    @Builder
    private OauthToken(Member member, String provider,
                       String accessToken, String refreshToken,
                       LocalDateTime accessTokenExpiresAt,
                       LocalDateTime refreshTokenExpiresAt) {
        this.member = member;
        this.provider = provider;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }


    // 토큰 갱신
    public void updateTokens(String accessToken, String refreshToken,
                             LocalDateTime accessTokenExpiresAt,
                             LocalDateTime refreshTokenExpiresAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }
}

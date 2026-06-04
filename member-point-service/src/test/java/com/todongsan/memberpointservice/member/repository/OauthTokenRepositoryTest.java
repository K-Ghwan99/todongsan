package com.todongsan.memberpointservice.member.repository;

import com.todongsan.memberpointservice.member.entity.Member;
import com.todongsan.memberpointservice.member.entity.MemberRole;
import com.todongsan.memberpointservice.member.entity.OauthToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OauthTokenRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OauthTokenRepository oauthTokenRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder()
                .nickname("테스트회원")
                .oauthProvider("KAKAO")
                .oauthId("kakao-001")
                .role(MemberRole.USER)
                .build());

        oauthTokenRepository.save(OauthToken.builder()
                .member(member)
                .provider("KAKAO")
                .accessToken("encrypted-access-token")
                .refreshToken("encrypted-refresh-token")
                .accessTokenExpiresAt(LocalDateTime.now().plusHours(6))
                .refreshTokenExpiresAt(LocalDateTime.now().plusDays(60))
                .build());
    }

    @Test
    void findByMemberId_존재하면_반환() {
        Optional<OauthToken> result = oauthTokenRepository.findByMemberId(member.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getProvider()).isEqualTo("KAKAO");
    }

    @Test
    void findByMemberId_없으면_empty반환() {
        Optional<OauthToken> result = oauthTokenRepository.findByMemberId(99999L);

        assertThat(result).isEmpty();
    }

    @Test
    void deleteByMemberId_삭제후_조회시_empty반환() {
        oauthTokenRepository.deleteByMemberId(member.getId());

        Optional<OauthToken> result = oauthTokenRepository.findByMemberId(member.getId());
        assertThat(result).isEmpty();
    }
}

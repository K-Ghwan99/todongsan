package com.todongsan.memberpointservice.member.repository;

import com.todongsan.memberpointservice.member.entity.Member;
import com.todongsan.memberpointservice.member.entity.MemberRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    private Member activeMember;
    private Member deletedMember;

    @BeforeEach
    void setUp() {
        activeMember = memberRepository.save(Member.builder()
                .nickname("활성회원")
                .oauthProvider("KAKAO")
                .oauthId("kakao-001")
                .role(MemberRole.USER)
                .build());

        deletedMember = memberRepository.save(Member.builder()
                .nickname("탈퇴회원")
                .oauthProvider("KAKAO")
                .oauthId("kakao-002")
                .role(MemberRole.USER)
                .build());
        deletedMember.delete(LocalDateTime.now());
        memberRepository.save(deletedMember);
    }

    @Test
    void findByIdAndDeletedAtIsNull_활성회원_정상반환() {
        Optional<Member> result = memberRepository.findByIdAndDeletedAtIsNull(activeMember.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getNickname()).isEqualTo("활성회원");
    }

    @Test
    void findByIdAndDeletedAtIsNull_탈퇴회원_empty반환() {
        Optional<Member> result = memberRepository.findByIdAndDeletedAtIsNull(deletedMember.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findByOauthProviderAndOauthId_일치하면_반환() {
        Optional<Member> result = memberRepository.findByOauthProviderAndOauthId("KAKAO", "kakao-001");

        assertThat(result).isPresent();
        assertThat(result.get().getOauthId()).isEqualTo("kakao-001");
    }

    @Test
    void findByOauthProviderAndOauthId_불일치하면_empty반환() {
        Optional<Member> result = memberRepository.findByOauthProviderAndOauthId("KAKAO", "없는아이디");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByNicknameAndDeletedAtIsNull_존재하는닉네임_true() {
        boolean result = memberRepository.existsByNicknameAndDeletedAtIsNull("활성회원");

        assertThat(result).isTrue();
    }

    @Test
    void existsByNicknameAndDeletedAtIsNull_탈퇴회원닉네임_false() {
        boolean result = memberRepository.existsByNicknameAndDeletedAtIsNull("탈퇴회원");

        assertThat(result).isFalse();
    }

    @Test
    void existsByNicknameAndDeletedAtIsNull_없는닉네임_false() {
        boolean result = memberRepository.existsByNicknameAndDeletedAtIsNull("없는닉네임");

        assertThat(result).isFalse();
    }

    @Test
    void findAllByIdIn_탈퇴회원포함_전체반환() {
        List<Member> result = memberRepository.findAllByIdIn(
                List.of(activeMember.getId(), deletedMember.getId()));

        assertThat(result).hasSize(2);
    }

    @Test
    void findAllByIdIn_없는ID_제외하고반환() {
        List<Member> result = memberRepository.findAllByIdIn(
                List.of(activeMember.getId(), 99999L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(activeMember.getId());
    }

    @Test
    void spendPoint_잔액충분_차감성공() {
        // 잔액 100 설정
        memberRepository.earnPoint(activeMember.getId(), BigDecimal.valueOf(100));

        int affected = memberRepository.spendPoint(activeMember.getId(), BigDecimal.valueOf(60));
        Member result = memberRepository.findById(activeMember.getId()).orElseThrow();

        assertThat(affected).isEqualTo(1);
        assertThat(result.getPointBalance()).isEqualByComparingTo(BigDecimal.valueOf(40));
    }

    @Test
    void spendPoint_잔액부족_0반환_잔액변화없음() {
        // 잔액 0인 상태에서 차감
        int affected = memberRepository.spendPoint(activeMember.getId(), BigDecimal.valueOf(100));
        Member result = memberRepository.findById(activeMember.getId()).orElseThrow();

        assertThat(affected).isEqualTo(0);
        assertThat(result.getPointBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void spendPoint_잔액과_차감액_동일_성공() {
        memberRepository.earnPoint(activeMember.getId(), BigDecimal.valueOf(50));

        int affected = memberRepository.spendPoint(activeMember.getId(), BigDecimal.valueOf(50));
        Member result = memberRepository.findById(activeMember.getId()).orElseThrow();

        assertThat(affected).isEqualTo(1);
        assertThat(result.getPointBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void earnPoint_정상적립_잔액증가() {
        int affected = memberRepository.earnPoint(activeMember.getId(), BigDecimal.valueOf(100));
        Member result = memberRepository.findById(activeMember.getId()).orElseThrow();

        assertThat(affected).isEqualTo(1);
        assertThat(result.getPointBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }
}

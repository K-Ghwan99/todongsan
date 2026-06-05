package com.todongsan.memberpointservice.member.service;

import com.todongsan.memberpointservice.member.dto.response.MemberBatchItemResponse;
import com.todongsan.memberpointservice.member.entity.AgeGroup;
import com.todongsan.memberpointservice.member.entity.Gender;
import com.todongsan.memberpointservice.member.entity.Member;
import com.todongsan.memberpointservice.member.repository.MemberRepository;
import com.todongsan.memberpointservice.member.repository.OauthTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberBatchServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock OauthTokenRepository oauthTokenRepository;

    @InjectMocks MemberServiceImpl memberServiceImpl;

    private Member createMember(String nickname, AgeGroup ageGroup, Gender gender,
                                String sido, String sigu) {
        Member member = Member.builder()
                .nickname(nickname)
                .email(nickname + "@kakao.com")
                .oauthProvider("KAKAO")
                .oauthId("kakao-" + nickname)
                .ageGroup(ageGroup)
                .gender(gender)
                .build();
        member.updateResidence(sido, sigu, LocalDateTime.now().minusDays(60));
        return member;
    }

    @Test
    void getBatch_정상_조회() {
        Member m1 = createMember("유저1", AgeGroup.AGE_20S, Gender.MALE, "서울특별시", "마포구");
        Member m2 = createMember("유저2", AgeGroup.AGE_30S, Gender.FEMALE, "경기도", "성남시");
        when(memberRepository.findAllByIdIn(List.of(1L, 2L))).thenReturn(List.of(m1, m2));

        List<MemberBatchItemResponse> result = memberServiceImpl.getBatch(List.of(1L, 2L));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAgeGroup()).isEqualTo("20대");
        assertThat(result.get(0).getGender()).isEqualTo("MALE");
        assertThat(result.get(0).getResidenceSido()).isEqualTo("서울특별시");
        assertThat(result.get(1).getAgeGroup()).isEqualTo("30대");
        assertThat(result.get(1).getGender()).isEqualTo("FEMALE");
    }

    @Test
    void getBatch_없는_memberId_제외() {
        Member m1 = createMember("유저1", AgeGroup.AGE_20S, Gender.MALE, "서울특별시", "마포구");
        // 999L은 존재하지 않아 결과에서 제외됨
        when(memberRepository.findAllByIdIn(List.of(1L, 999L))).thenReturn(List.of(m1));

        List<MemberBatchItemResponse> result = memberServiceImpl.getBatch(List.of(1L, 999L));

        assertThat(result).hasSize(1);
    }

    @Test
    void getBatch_탈퇴회원_포함() {
        Member active = createMember("활성유저", AgeGroup.AGE_20S, Gender.MALE, "서울특별시", "마포구");
        Member deleted = createMember("탈퇴유저", AgeGroup.AGE_40S, Gender.FEMALE, "부산광역시", "해운대구");
        deleted.delete(LocalDateTime.now().minusDays(1));

        when(memberRepository.findAllByIdIn(List.of(1L, 2L))).thenReturn(List.of(active, deleted));

        List<MemberBatchItemResponse> result = memberServiceImpl.getBatch(List.of(1L, 2L));

        assertThat(result).hasSize(2);
    }

    @Test
    void getBatch_ageGroup_null이면_UNKNOWN() {
        Member member = createMember("유저", null, null, null, null);
        when(memberRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(member));

        List<MemberBatchItemResponse> result = memberServiceImpl.getBatch(List.of(1L));

        assertThat(result.get(0).getAgeGroup()).isEqualTo("UNKNOWN");
        assertThat(result.get(0).getGender()).isEqualTo("UNKNOWN");
    }

    @Test
    void getBatch_빈_결과() {
        when(memberRepository.findAllByIdIn(List.of(999L))).thenReturn(List.of());

        List<MemberBatchItemResponse> result = memberServiceImpl.getBatch(List.of(999L));

        assertThat(result).isEmpty();
    }
}

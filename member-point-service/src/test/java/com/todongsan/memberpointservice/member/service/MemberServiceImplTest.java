package com.todongsan.memberpointservice.member.service;

import com.todongsan.memberpointservice.global.exception.CustomException;
import com.todongsan.memberpointservice.global.exception.ErrorCode;
import com.todongsan.memberpointservice.member.dto.request.MemberUpdateRequest;
import com.todongsan.memberpointservice.member.dto.response.MemberResponse;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock MemberRepository memberRepository;
    @Mock OauthTokenRepository oauthTokenRepository;

    @InjectMocks MemberServiceImpl memberServiceImpl;

    private Member createMember(String nickname) {
        return Member.builder()
                .nickname(nickname)
                .email(nickname + "@kakao.com")
                .oauthProvider("KAKAO")
                .oauthId("kakao-" + nickname)
                .ageGroup(AgeGroup.AGE_20S)
                .gender(Gender.MALE)
                .build();
    }

    // --- getMe ---

    @Test
    void getMe_성공() {
        Member member = createMember("홍길동");
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));

        MemberResponse response = memberServiceImpl.getMe(1L);

        assertThat(response.getNickname()).isEqualTo("홍길동");
        assertThat(response.getEmail()).isEqualTo("홍길동@kakao.com");
        assertThat(response.getRole()).isEqualTo("USER");
    }

    @Test
    void getMe_없는회원_MEMBER_NOT_FOUND() {
        when(memberRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberServiceImpl.getMe(99L))
                .isInstanceOf(CustomException.class)
                .satisfies(ex ->
                        assertThat(((CustomException) ex).getErrorCode())
                                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
    }

    // --- updateMe ---

    @Test
    void updateMe_닉네임_변경_성공() {
        Member member = createMember("기존닉네임");
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNicknameAndDeletedAtIsNull("새닉네임")).thenReturn(false);

        MemberUpdateRequest request = mock(MemberUpdateRequest.class);
        when(request.getNickname()).thenReturn("새닉네임");
        when(request.getResidenceSido()).thenReturn(null);
        when(request.getResidenceSigu()).thenReturn(null);

        memberServiceImpl.updateMe(1L, request);

        assertThat(member.getNickname()).isEqualTo("새닉네임");
    }

    @Test
    void updateMe_닉네임_중복_MEMBER_NICKNAME_DUPLICATE() {
        Member member = createMember("기존닉네임");
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNicknameAndDeletedAtIsNull("중복닉네임")).thenReturn(true);

        MemberUpdateRequest request = mock(MemberUpdateRequest.class);
        when(request.getNickname()).thenReturn("중복닉네임");

        assertThatThrownBy(() -> memberServiceImpl.updateMe(1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(ex ->
                        assertThat(((CustomException) ex).getErrorCode())
                                .isEqualTo(ErrorCode.MEMBER_NICKNAME_DUPLICATE));
    }

    @Test
    void updateMe_거주지_최초설정_쿨다운_미적용() {
        Member member = createMember("홍길동");
        // residenceChangedAt = null → 최초 설정, 쿨다운 없음
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));

        MemberUpdateRequest request = mock(MemberUpdateRequest.class);
        when(request.getNickname()).thenReturn(null);
        when(request.getResidenceSido()).thenReturn("서울특별시");
        when(request.getResidenceSigu()).thenReturn("마포구");

        memberServiceImpl.updateMe(1L, request);

        assertThat(member.getResidenceSido()).isEqualTo("서울특별시");
        assertThat(member.getResidenceSigu()).isEqualTo("마포구");
        assertThat(member.getResidenceChangedAt()).isNotNull();
    }

    @Test
    void updateMe_거주지_30일이내_MEMBER_RESIDENCE_CHANGE_COOLDOWN() {
        Member member = createMember("홍길동");
        member.updateResidence("서울특별시", "마포구", LocalDateTime.now().minusDays(10));
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));

        MemberUpdateRequest request = mock(MemberUpdateRequest.class);
        when(request.getNickname()).thenReturn(null);
        when(request.getResidenceSido()).thenReturn("부산광역시");

        assertThatThrownBy(() -> memberServiceImpl.updateMe(1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(ex ->
                        assertThat(((CustomException) ex).getErrorCode())
                                .isEqualTo(ErrorCode.MEMBER_RESIDENCE_CHANGE_COOLDOWN));
    }

    @Test
    void updateMe_거주지_30일이후_변경_성공() {
        Member member = createMember("홍길동");
        member.updateResidence("서울특별시", "마포구", LocalDateTime.now().minusDays(31));
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));

        MemberUpdateRequest request = mock(MemberUpdateRequest.class);
        when(request.getNickname()).thenReturn(null);
        when(request.getResidenceSido()).thenReturn("부산광역시");
        when(request.getResidenceSigu()).thenReturn("해운대구");

        memberServiceImpl.updateMe(1L, request);

        assertThat(member.getResidenceSido()).isEqualTo("부산광역시");
        assertThat(member.getResidenceSigu()).isEqualTo("해운대구");
    }

    // --- withdraw ---

    @Test
    void withdraw_성공() {
        Member member = createMember("홍길동");
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));

        Long result = memberServiceImpl.withdraw(1L);

        assertThat(result).isEqualTo(1L);
        verify(oauthTokenRepository).deleteByMemberId(1L);
        assertThat(member.isDeleted()).isTrue();
    }

    @Test
    void withdraw_없는회원_MEMBER_NOT_FOUND() {
        when(memberRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberServiceImpl.withdraw(99L))
                .isInstanceOf(CustomException.class)
                .satisfies(ex ->
                        assertThat(((CustomException) ex).getErrorCode())
                                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
    }
}

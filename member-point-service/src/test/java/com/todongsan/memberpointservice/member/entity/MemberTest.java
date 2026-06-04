package com.todongsan.memberpointservice.member.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

    // isDeleted() — deletedAt null이면 false
    @Test
    void isDeleted_whenDeletedAtIsNull_returnsFalse() {
        Member member = Member.builder()
                .nickname("테스터")
                .oauthProvider("KAKAO")
                .oauthId("12345")
                .build();

        assertThat(member.isDeleted()).isFalse();
    }

    // isDeleted() — deletedAt 있으면 true
    @Test
    void isDeleted_whenDeletedAtIsSet_returnsTrue() {
        Member member = Member.builder()
                .nickname("테스터")
                .oauthProvider("KAKAO")
                .oauthId("12345")
                .build();

        member.delete(LocalDateTime.now());

        assertThat(member.isDeleted()).isTrue();
    }
}

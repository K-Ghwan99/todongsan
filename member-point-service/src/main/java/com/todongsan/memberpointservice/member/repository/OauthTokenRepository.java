package com.todongsan.memberpointservice.member.repository;

import com.todongsan.memberpointservice.member.entity.OauthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface OauthTokenRepository extends JpaRepository<OauthToken, Long> {

    // 회원 ID로 토큰 조회
    Optional<OauthToken> findByMemberId(Long memberId);

    // 회원 탈퇴 시 토큰 물리 삭제
    @Transactional
    void deleteByMemberId(Long memberId);
}

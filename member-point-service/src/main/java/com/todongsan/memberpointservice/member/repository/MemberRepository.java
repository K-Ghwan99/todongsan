package com.todongsan.memberpointservice.member.repository;

import com.todongsan.memberpointservice.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 탈퇴 회원 제외 단건 조회 (일반 API용)
    Optional<Member> findByIdAndDeletedAtIsNull(Long id);

    // OAuth 로그인 조회
    Optional<Member> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);

    // 닉네임 중복 체크 (탈퇴 회원 제외)
    boolean existsByNicknameAndDeletedAtIsNull(String nickname);

    // 배치 조회 (탈퇴 회원 포함)
    List<Member> findAllByIdIn(List<Long> ids);

    // 포인트 차감 (잔액 부족 시 0 반환)
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Member m SET m.pointBalance = m.pointBalance - :amount " +
            "WHERE m.id = :memberId AND m.pointBalance >= :amount")
    int spendPoint(@Param("memberId") Long memberId, @Param("amount") BigDecimal amount);

    // 포인트 적립/정산/환불 (memberId 없으면 0 반환)
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Member m SET m.pointBalance = m.pointBalance + :amount " +
            "WHERE m.id = :memberId")
    int earnPoint(@Param("memberId") Long memberId, @Param("amount") BigDecimal amount);
}

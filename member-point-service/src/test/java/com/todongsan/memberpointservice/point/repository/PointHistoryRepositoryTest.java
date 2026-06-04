package com.todongsan.memberpointservice.point.repository;

import com.todongsan.memberpointservice.member.entity.Member;
import com.todongsan.memberpointservice.member.entity.MemberRole;
import com.todongsan.memberpointservice.member.repository.MemberRepository;
import com.todongsan.memberpointservice.point.entity.PointHistory;
import com.todongsan.memberpointservice.point.entity.PointHistoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PointHistoryRepositoryTest {

    @Autowired
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Long memberId;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.save(Member.builder()
                .nickname("테스트회원")
                .oauthProvider("KAKAO")
                .oauthId("kakao-001")
                .role(MemberRole.USER)
                .build());
        memberId = member.getId();

        // EARN 이력 3건
        for (int i = 1; i <= 3; i++) {
            pointHistoryRepository.save(PointHistory.builder()
                    .memberId(memberId)
                    .type(PointHistoryType.EARN_VOTE)
                    .amount(BigDecimal.valueOf(10))
                    .balanceSnapshot(BigDecimal.valueOf(i * 10))
                    .idempotencyKey("earn-key-" + i)
                    .build());
        }

        // SPEND 이력 2건
        for (int i = 1; i <= 2; i++) {
            pointHistoryRepository.save(PointHistory.builder()
                    .memberId(memberId)
                    .type(PointHistoryType.SPEND_MARKET)
                    .amount(BigDecimal.valueOf(5))
                    .balanceSnapshot(BigDecimal.valueOf(25))
                    .idempotencyKey("spend-key-" + i)
                    .build());
        }
    }

    @Test
    void findByIdempotencyKey_존재하면_반환() {
        Optional<PointHistory> result = pointHistoryRepository.findByIdempotencyKey("earn-key-1");

        assertThat(result).isPresent();
        assertThat(result.get().getType()).isEqualTo(PointHistoryType.EARN_VOTE);
    }

    @Test
    void findByIdempotencyKey_없으면_empty반환() {
        Optional<PointHistory> result = pointHistoryRepository.findByIdempotencyKey("없는키");

        assertThat(result).isEmpty();
    }

    @Test
    void findByMemberId_전체이력_페이징() {
        PageRequest pageable = PageRequest.of(0, 2, Sort.by("createdAt").descending());

        Page<PointHistory> result = pointHistoryRepository.findByMemberId(memberId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }

    @Test
    void findByMemberId_이력없는회원_empty() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        Page<PointHistory> result = pointHistoryRepository.findByMemberId(99999L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }
}

package com.todongsan.memberpointservice.member.entity;

import com.todongsan.memberpointservice.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "member",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"oauth_provider", "oauth_id"}),
                @UniqueConstraint(columnNames = "nickname")
        }
)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    // 닉네임 (중복 불가)
    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    // 포인트 잔액 (음수 불가, DB CHECK 제약)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pointBalance = BigDecimal.ZERO;

    // 회원 권한
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role = MemberRole.USER;

    @Column(length = 50)
    private String residenceSido;

    @Column(length = 50)
    private String residenceSigu;

    // 거주지 마지막 변경 시각 (30일 쿨다운 추적)
    private LocalDateTime residenceChangedAt;

    // 연령대 (카카오 birthyear 기반)
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AgeGroup ageGroup;

    // 성별 (카카오 gender 기반)
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    // OAuth 제공자 (KAKAO)
    @Column(nullable = false, length = 20)
    private String oauthProvider;

    // OAuth 제공자 고유 ID
    @Column(nullable = false, length = 255)
    private String oauthId;

    // 탈퇴 시각(null이면 활성 회원)
    private LocalDateTime deletedAt;

    @Builder
    private Member(String email, String nickname, MemberRole role,
                   String oauthProvider, String oauthId,
                   AgeGroup ageGroup, Gender gender) {
        this.email = email;
        this.nickname = nickname;
        this.role = role != null ? role : MemberRole.USER;
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
        this.ageGroup = ageGroup;
        this.gender = gender;
        this.pointBalance = BigDecimal.ZERO;
    }

    // 탈퇴 여부 확인
    public boolean isDeleted() {
        return deletedAt != null;
    }

    // 닉네임 변경
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    // 거주지 변경
    public void updateResidence(String sido, String sigu, LocalDateTime changedAt) {
        this.residenceSido = sido;
        this.residenceSigu = sigu;
        this.residenceChangedAt = changedAt;
    }

    // 소프트 삭제
    public void delete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

}

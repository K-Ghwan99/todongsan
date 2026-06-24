package com.todongsan.battle_service.vote.entity;

import com.todongsan.battle_service.battle.entity.Battle;
import com.todongsan.battle_service.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "battle_vote",
        uniqueConstraints = @UniqueConstraint(name = "uq_battle_vote", columnNames = {"battle_id", "member_id"}))
@Getter
@NoArgsConstructor
public class BattleVote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "battle_id", nullable = false)
    private Long battleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id", nullable = false, insertable = false, updatable = false)
    private Battle battle;

    @Column(name = "member_id", nullable = false)
    private Long memberId; // member.id (REST 참조)

    @Column(name = "selected_option", nullable = false, length = 1)
    private String selectedOption; // 'A' or 'B'

    @Column(name = "is_rewarded", nullable = false)
    private boolean isRewarded = false;

    @Builder
    public BattleVote(Long battleId, Long memberId, String selectedOption) {
        this.battleId = battleId;
        this.memberId = memberId;
        this.selectedOption = selectedOption;
        this.isRewarded = false;
    }

    public void markRewarded() {
        this.isRewarded = true;
    }
}

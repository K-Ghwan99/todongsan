package com.todongsan.battle_service.comment.entity;

import com.todongsan.battle_service.battle.entity.Battle;
import com.todongsan.battle_service.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "comment", indexes = {
        @Index(name = "idx_comment_battle", columnList = "battle_id, deleted_at, created_at")
})
@Getter
@NoArgsConstructor
public class Comment extends BaseEntity {

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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Comment(Long battleId, Long memberId, String content) {
        this.battleId = battleId;
        this.memberId = memberId;
        this.content = content;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}

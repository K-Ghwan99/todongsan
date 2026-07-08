package com.todongsan.battle_service.comment.repository;

import com.todongsan.battle_service.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByBattleIdAndDeletedAtIsNull(Long battleId, Pageable pageable);

    Optional<Comment> findByIdAndDeletedAtIsNull(Long id);

    long countByBattleIdAndDeletedAtIsNull(Long battleId);
}

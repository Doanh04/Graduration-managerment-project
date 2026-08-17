package com.graduration.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.graduration.entity.CommentEntity;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    @Query(
            """
			select comment from CommentEntity comment
			where comment.submistion.IdSubmission = :submissionId
			and comment.deletedAt is null
			order by comment.createdAt asc
			""")
    Page<CommentEntity> findBySubmistion_IdSubmissionOrderByCreatedAtAsc(
            @Param("submissionId") Long submissionId, Pageable pageable);

    @Query(
            "select comment from CommentEntity comment where comment.idComment = :commentId and comment.deletedAt is null")
    Optional<CommentEntity> findActiveById(@Param("commentId") Long commentId);
}

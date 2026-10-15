package com.graduration.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.graduration.Constain.ScoreTypeConstain;
import com.graduration.entity.ScoreEntity;

public interface ScoreRepository extends JpaRepository<ScoreEntity, Long> {
    boolean existsByTopic_IdTopic(Long topicId);
    boolean existsByAutomaticFinalTopicIdAndStudent_IdStudent(Long topicId, String studentId);

    @Query("""
            select distinct score.topic.idTopic from ScoreEntity score
            where score.lecture is not null
            and score.status = com.graduration.Constain.ScoreStatusConstain.LOCKED
            and score.publishedAt is null
            and score.topic.defensePeriod.finalSubmissionDeadline is not null
            and score.topic.defensePeriod.finalSubmissionDeadline < :now
            and not exists (
                select published.id from ScoreEntity published
                where published.topic.idTopic = score.topic.idTopic
                and published.publishedAt is not null
            )
            """)
    List<Long> findTopicsAwaitingFinalPastDeadline(@Param("now") LocalDateTime now);
    @EntityGraph(attributePaths = {"student", "topic", "topic.team", "lecture"})
    Optional<ScoreEntity> findWithRelationsById(Long id);

    @Query(
            """
			select score from ScoreEntity score
			where score.student.idStudent = :studentId
			and score.topic.idTopic = :topicId
			and score.lecture.lectureId = :lectureId
			and score.scoreType = :scoreType
			""")
    Optional<ScoreEntity> findStudentTopicScore(
            @Param("studentId") String studentId,
            @Param("topicId") Long topicId,
            @Param("lectureId") String lectureId,
            @Param("scoreType") ScoreTypeConstain scoreType);

    @EntityGraph(attributePaths = {"student", "topic", "topic.team", "lecture"})
    @Query("""
            select score from ScoreEntity score
            where score.student.idStudent = :studentId
            and score.topic.idTopic = :topicId
            and score.lecture is not null
            and score.scoreType = com.graduration.Constain.ScoreTypeConstain.FINAL
            order by score.lecture.fullNameLecture
            """)
    List<ScoreEntity> findCommitteeScores(
            @Param("studentId") String studentId,
            @Param("topicId") Long topicId);

    @Query(
            value =
                    """
				select score from ScoreEntity score
				where score.topic.defensePeriod.ID_Defense = :defensePeriodId
				order by score.student.studentCode asc
				""",
            countQuery =
                    """
				select count(score) from ScoreEntity score
				where score.topic.defensePeriod.ID_Defense = :defensePeriodId
				""")
    Page<ScoreEntity> findByDefensePeriod(@Param("defensePeriodId") Long defensePeriodId, Pageable pageable);

    @Query(
            """
			select score from ScoreEntity score
			where score.student.idStudent = :studentId
			and score.topic.defensePeriod.ID_Defense = :defensePeriodId
			""")
    List<ScoreEntity> findByStudentAndDefensePeriod(
            @Param("studentId") String studentId, @Param("defensePeriodId") Long defensePeriodId);

    @Query(
            """
			select score from ScoreEntity score
			where score.student.idStudent = :studentId
			and score.status in (
				com.graduration.Constain.ScoreStatusConstain.SUBMITTED,
				com.graduration.Constain.ScoreStatusConstain.LOCKED
			)
			and score.publishedAt is not null
			order by score.topic.defensePeriod.endDate desc, score.updatedAt desc
			""")
    List<ScoreEntity> findPublishedByStudent(@Param("studentId") String studentId);
}

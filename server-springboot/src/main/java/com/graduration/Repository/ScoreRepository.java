package com.graduration.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.graduration.Constain.ScoreTypeConstain;
import com.graduration.entity.ScoreEntity;

public interface ScoreRepository extends JpaRepository<ScoreEntity, Long> {
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
}

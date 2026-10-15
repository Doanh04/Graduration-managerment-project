package com.graduration.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.graduration.Constain.FinalSubmissionStatusConstain;
import com.graduration.entity.FinalSubmissionEntity;

public interface FinalSubmissionRepository extends JpaRepository<FinalSubmissionEntity, Long> {
    @EntityGraph(attributePaths = {"topic", "topic.team", "topic.defensePeriod", "topic.defensePeriod.academicYear"})
    List<FinalSubmissionEntity> findByTopic_IdTopicOrderByRevisionDesc(Long topicId);

    Optional<FinalSubmissionEntity> findFirstByTopic_IdTopicOrderByRevisionDesc(Long topicId);

    @Query("""
            select submission
            from FinalSubmissionEntity submission
            join fetch submission.topic topic
            left join fetch topic.team team
            join fetch topic.defensePeriod period
            left join fetch period.academicYear academicYear
            where period.ID_Defense = :defensePeriodId
            order by topic.idTopic asc, submission.revision asc
            """)
    List<FinalSubmissionEntity> findAllForBackup(@Param("defensePeriodId") Long defensePeriodId);

    boolean existsByTopic_IdTopicAndStatus(Long topicId, FinalSubmissionStatusConstain status);
}

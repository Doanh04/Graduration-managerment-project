package com.graduration.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.graduration.Constain.SupervisorAssignmentStatusConstain;
import com.graduration.Constain.SupervisorRoleConstain;
import com.graduration.entity.TopicSuperVisorEntity;

@Repository
public interface TopicSupervisorRepository extends JpaRepository<TopicSuperVisorEntity, Long> {
    boolean existsByTopic_IdTopicAndLecture_LectureIdAndStatus(
            Long topicId, String lectureId, SupervisorAssignmentStatusConstain status);

    boolean existsByTopic_IdTopicAndSupervisorRoleAndStatus(
            Long topicId, SupervisorRoleConstain role, SupervisorAssignmentStatusConstain status);

    boolean existsByTopic_IdTopicAndSupervisorRoleAndStatusAndIdSuperVisorNot(
            Long topicId, SupervisorRoleConstain role, SupervisorAssignmentStatusConstain status, Long assignmentId);

    @Override
    @EntityGraph(attributePaths = {"topic", "topic.defensePeriod", "lecture", "lecture.user", "assignedBy"})
    Optional<TopicSuperVisorEntity> findById(Long assignmentId);

    @EntityGraph(attributePaths = {"topic", "topic.defensePeriod", "lecture", "lecture.user", "assignedBy"})
    Page<TopicSuperVisorEntity> findByTopic_IdTopic(Long topicId, Pageable pageable);

    @EntityGraph(attributePaths = {"topic", "topic.defensePeriod", "lecture", "lecture.user", "assignedBy"})
    Page<TopicSuperVisorEntity> findByLecture_LectureIdAndStatus(
            String lectureId, SupervisorAssignmentStatusConstain status, Pageable pageable);

    @Query(
            """
			select count(ts) from TopicSuperVisorEntity ts
			where ts.lecture.lectureId = :lectureId
			and ts.topic.defensePeriod.ID_Defense = :defensePeriodId
			and ts.status = :status
			""")
    long countActiveAssignments(
            @Param("lectureId") String lectureId,
            @Param("defensePeriodId") Long defensePeriodId,
            @Param("status") SupervisorAssignmentStatusConstain status);
}

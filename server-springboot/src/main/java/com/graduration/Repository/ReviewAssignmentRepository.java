package com.graduration.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.graduration.Constain.ReviewAssignmentStatusConstain;
import com.graduration.entity.ReviewAssignmentEntity;

@Repository
public interface ReviewAssignmentRepository extends JpaRepository<ReviewAssignmentEntity, Long> {
    @Override
    @EntityGraph(
            attributePaths = {
                "topic",
                "topic.defensePeriod",
                "topic.team",
                "lecture",
                "lecture.user",
                "assignedBy",
                "reviewedBy"
            })
    Optional<ReviewAssignmentEntity> findById(Long assignmentId);

    @Query(
            value =
                    """
					select assignment from ReviewAssignmentEntity assignment
					where assignment.topic.idTopic = :topicId
					and (:status is null or assignment.status = :status)
					order by assignment.assignedAt desc
					""",
            countQuery =
                    """
					select count(assignment) from ReviewAssignmentEntity assignment
					where assignment.topic.idTopic = :topicId
					and (:status is null or assignment.status = :status)
					""")
    Page<ReviewAssignmentEntity> findByTopic(
            @Param("topicId") Long topicId, @Param("status") ReviewAssignmentStatusConstain status, Pageable pageable);

    @Query(
            value =
                    """
					select assignment from ReviewAssignmentEntity assignment
					where assignment.lecture.lectureId = :lectureId
					and (:status is null or assignment.status = :status)
					order by assignment.deadline, assignment.assignedAt desc
					""",
            countQuery =
                    """
					select count(assignment) from ReviewAssignmentEntity assignment
					where assignment.lecture.lectureId = :lectureId
					and (:status is null or assignment.status = :status)
					""")
    Page<ReviewAssignmentEntity> findByLecturer(
            @Param("lectureId") String lectureId,
            @Param("status") ReviewAssignmentStatusConstain status,
            Pageable pageable);

    @Query(
            """
			select count(assignment) > 0 from ReviewAssignmentEntity assignment
			where assignment.topic.idTopic = :topicId
			and assignment.lecture.lectureId = :lectureId
			and assignment.status <> :cancelled
			""")
    boolean existsActiveAssignment(
            @Param("topicId") Long topicId,
            @Param("lectureId") String lectureId,
            @Param("cancelled") ReviewAssignmentStatusConstain cancelled);

    @Query(
            """
			select count(assignment) from ReviewAssignmentEntity assignment
			where assignment.topic.idTopic = :topicId
			and assignment.status <> :cancelled
			""")
    long countForTopic(@Param("topicId") Long topicId, @Param("cancelled") ReviewAssignmentStatusConstain cancelled);

    @Query(
            """
			select count(assignment) from ReviewAssignmentEntity assignment
			where assignment.lecture.lectureId = :lectureId
			and assignment.topic.defensePeriod.ID_Defense = :periodId
			and assignment.status <> :cancelled
			""")
    long countForLecturerInPeriod(
            @Param("lectureId") String lectureId,
            @Param("periodId") Long periodId,
            @Param("cancelled") ReviewAssignmentStatusConstain cancelled);

    boolean existsByTopic_IdTopicAndStatus(Long topicId, ReviewAssignmentStatusConstain status);
}

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
                "defenseCommittee",
                "defenseCommittee.comitteesMember",
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
            value = """
                    select distinct assignment from ReviewAssignmentEntity assignment
                    join assignment.defenseCommittee committee
                    join committee.comitteesMember member
                    where member.lecture.user.userId = :userId
                    and member.role = com.graduration.Constain.CommitteeMemberRoleConstain.REVIEWER
                    and member.status = com.graduration.Constain.CommitteeMemberStatusConstain.ACTIVE
                    and committee.status <> com.graduration.Constain.DefenseCommitteeStatusConstain.INACTIVE
                    and (:status is null or assignment.status = :status)
                    order by assignment.deadline, assignment.assignedAt desc
                    """,
            countQuery = """
                    select count(distinct assignment) from ReviewAssignmentEntity assignment
                    join assignment.defenseCommittee committee
                    join committee.comitteesMember member
                    where member.lecture.user.userId = :userId
                    and member.role = com.graduration.Constain.CommitteeMemberRoleConstain.REVIEWER
                    and member.status = com.graduration.Constain.CommitteeMemberStatusConstain.ACTIVE
                    and committee.status <> com.graduration.Constain.DefenseCommitteeStatusConstain.INACTIVE
                    and (:status is null or assignment.status = :status)
                    """)
    Page<ReviewAssignmentEntity> findByReviewerUser(
            @Param("userId") String userId,
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

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select assignment from ReviewAssignmentEntity assignment where assignment.reviewAssignmentId = :id")
    Optional<ReviewAssignmentEntity> lockForReport(@Param("id") Long id);

    @Query("""
            select distinct assignment from ReviewAssignmentEntity assignment
            join assignment.topic.team.studentEntities student
            join assignment.defenseCommittee committee
            where student.userEntity.userId = :userId
            and assignment.status <> com.graduration.Constain.ReviewAssignmentStatusConstain.CANCELLED
            and committee.status <> com.graduration.Constain.DefenseCommitteeStatusConstain.INACTIVE
            and exists (
                select member.comitteesMemberId from ComitteesMemberEntity member
                where member.defenseCommittees.idComittees = committee.idComittees
                and member.role = com.graduration.Constain.CommitteeMemberRoleConstain.REVIEWER
                and member.status = com.graduration.Constain.CommitteeMemberStatusConstain.ACTIVE
            )
            order by assignment.deadline desc, assignment.assignedAt desc
            """)
    java.util.List<ReviewAssignmentEntity> findStudentReportAssignments(@Param("userId") String userId);

    @Query("select assignment.reviewAssignmentId from ReviewAssignmentEntity assignment where assignment.reportCheckedAt is null and assignment.deadline <= :now and assignment.status in (com.graduration.Constain.ReviewAssignmentStatusConstain.ASSIGNED, com.graduration.Constain.ReviewAssignmentStatusConstain.IN_PROGRESS, com.graduration.Constain.ReviewAssignmentStatusConstain.REVISION_REQUIRED, com.graduration.Constain.ReviewAssignmentStatusConstain.SUBMITTED)")
    java.util.List<Long> findDueReportAssignments(@Param("now") java.time.LocalDateTime now);

    boolean existsByTopic_IdTopicAndDefenseCommittee_IdComitteesAndStatus(
            Long topicId, Long committeeId, ReviewAssignmentStatusConstain status);

    boolean existsByTopic_IdTopicAndDefenseCommittee_IdComitteesAndStatusNot(
            Long topicId, Long committeeId, ReviewAssignmentStatusConstain status);

    @EntityGraph(
            attributePaths = {
                "lecture",
                "defenseCommittee",
                "defenseCommittee.comitteesMember",
                "defenseCommittee.comitteesMember.lecture"
            })
    Optional<ReviewAssignmentEntity>
            findFirstByTopic_IdTopicAndDefenseCommittee_IdComitteesAndStatusOrderByReviewedAtDesc(
                    Long topicId, Long committeeId, ReviewAssignmentStatusConstain status);
}

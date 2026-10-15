package com.graduration.Repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.graduration.Constain.CommitteeMemberRoleConstain;
import com.graduration.Constain.CommitteeMemberStatusConstain;
import com.graduration.entity.ComitteesMemberEntity;
import com.graduration.entity.DefenseCommitteesEntity;

@Repository
public interface CommitteeMemberRepository extends JpaRepository<ComitteesMemberEntity, Long> {
    boolean existsByDefenseCommittees_IdComitteesAndLecture_LectureIdAndStatus(
            Long committeeId, String lectureId, CommitteeMemberStatusConstain status);

    boolean existsByDefenseCommittees_IdComitteesAndRoleAndStatus(
            Long committeeId, CommitteeMemberRoleConstain role, CommitteeMemberStatusConstain status);

    boolean existsByDefenseCommittees_IdComitteesAndRoleAndStatusAndComitteesMemberIdNot(
            Long committeeId, CommitteeMemberRoleConstain role, CommitteeMemberStatusConstain status, Long memberId);

    @Override
    @EntityGraph(
            attributePaths = {
                "defenseCommittees",
                "defenseCommittees.defensePeriod",
                "lecture",
                "lecture.user",
                "assignedBy"
            })
    Optional<ComitteesMemberEntity> findById(Long memberId);

    @EntityGraph(
            attributePaths = {
                "defenseCommittees",
                "defenseCommittees.defensePeriod",
                "lecture",
                "lecture.user",
                "assignedBy"
            })
    Page<ComitteesMemberEntity> findByDefenseCommittees_IdComittees(Long committeeId, Pageable pageable);

    @EntityGraph(
            attributePaths = {
                "defenseCommittees",
                "defenseCommittees.defensePeriod",
                "lecture",
                "lecture.user",
                "assignedBy"
            })
    Page<ComitteesMemberEntity> findByLecture_LectureIdAndStatus(
            String lectureId, CommitteeMemberStatusConstain status, Pageable pageable);

    long countByDefenseCommittees_IdComitteesAndStatus(Long committeeId, CommitteeMemberStatusConstain status);

    long countByDefenseCommittees_IdComitteesAndRoleAndStatus(
            Long committeeId, CommitteeMemberRoleConstain role, CommitteeMemberStatusConstain status);

    @EntityGraph(attributePaths = {"lecture", "lecture.user"})
    List<ComitteesMemberEntity> findByDefenseCommittees_IdComitteesAndRoleAndStatus(
            Long committeeId, CommitteeMemberRoleConstain role, CommitteeMemberStatusConstain status);

    @EntityGraph(attributePaths = {"lecture", "lecture.user", "defenseCommittees"})
    List<ComitteesMemberEntity> findByDefenseCommittees_IdComitteesAndStatusOrderByRoleAsc(
            Long committeeId, CommitteeMemberStatusConstain status);

    @Query("""
            select distinct member.defenseCommittees from ComitteesMemberEntity member
            where member.lecture.user.userId = :userId
            and member.role = :role
            and member.status = :status
            and member.defenseCommittees.status <> com.graduration.Constain.DefenseCommitteeStatusConstain.INACTIVE
            """)
    List<DefenseCommitteesEntity> findCommitteesForReviewerUser(
            @Param("userId") String userId,
            @Param("role") CommitteeMemberRoleConstain role,
            @Param("status") CommitteeMemberStatusConstain status);

    @Query("""
            select distinct member.defenseCommittees from ComitteesMemberEntity member
            where member.defenseCommittees.defensePeriod.ID_Defense in :periodIds
            and member.role = :role
            and member.status = :status
            and member.defenseCommittees.status <> com.graduration.Constain.DefenseCommitteeStatusConstain.INACTIVE
            """)
    List<DefenseCommitteesEntity> findReviewerCommitteesByPeriods(
            @Param("periodIds") java.util.Collection<Long> periodIds,
            @Param("role") CommitteeMemberRoleConstain role,
            @Param("status") CommitteeMemberStatusConstain status);

    @Query(
            """
			select count(member) > 0
			from ComitteesMemberEntity member
			join member.defenseCommittees committee
			join committee.defenseSchedules schedule
			where schedule.topic.idTopic = :topicId
			and member.lecture.lectureId = :lectureId
			and member.role = :role
			and member.status = :status
			""")
    boolean existsByTopicAndLecturerAndRoleAndStatus(
            @Param("topicId") Long topicId,
            @Param("lectureId") String lectureId,
            @Param("role") CommitteeMemberRoleConstain role,
            @Param("status") CommitteeMemberStatusConstain status);
}

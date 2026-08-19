package com.graduration.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.graduration.Constain.CommitteeMemberRoleConstain;
import com.graduration.Constain.CommitteeMemberStatusConstain;
import com.graduration.entity.ComitteesMemberEntity;

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
}

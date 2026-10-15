package com.graduration.Service.ManagerService;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Constain.CommitteeMemberRoleConstain;
import com.graduration.Constain.CommitteeMemberStatusConstain;
import com.graduration.Constain.DefenseCommitteeStatusConstain;
import com.graduration.Constain.ReviewAssignmentStatusConstain;
import com.graduration.Constain.TopicStatusConstain;
import com.graduration.Repository.CommitteeMemberRepository;
import com.graduration.Repository.DefenseCommitteeRepository;
import com.graduration.Repository.ReviewAssignmentRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.entity.DefenseCommitteesEntity;
import com.graduration.entity.ReviewAssignmentEntity;
import com.graduration.entity.TopicEntity;

import lombok.RequiredArgsConstructor;

/** Tự tạo hồ sơ phản biện từ vai trò REVIEWER trong hội đồng, không cần quản trị phân công thủ công. */
@Service
@RequiredArgsConstructor
public class ReviewAssignmentProvisioningService {
    private static final List<TopicStatusConstain> REVIEWABLE_STATUSES = List.of(
            TopicStatusConstain.APPROVED,
            TopicStatusConstain.REGISTERED,
            TopicStatusConstain.IN_PROGRESS);

    private final DefenseCommitteeRepository committees;
    private final CommitteeMemberRepository members;
    private final TopicRepository topics;
    private final ReviewAssignmentRepository assignments;

    @Transactional
    public void provisionCommittee(Long committeeId) {
        committees.findById(committeeId).ifPresent(this::provisionCommittee);
    }

    @Transactional
    public void provisionForReviewerUser(String userId) {
        members.findCommitteesForReviewerUser(
                        userId, CommitteeMemberRoleConstain.REVIEWER, CommitteeMemberStatusConstain.ACTIVE)
                .forEach(this::provisionCommittee);
    }

    @Transactional
    public void provisionForStudentUser(String userId) {
        List<TopicEntity> studentTopics = topics.findReviewableByStudentUser(userId, REVIEWABLE_STATUSES);
        Collection<Long> periodIds = studentTopics.stream()
                .map(TopicEntity::getDefensePeriod)
                .filter(java.util.Objects::nonNull)
                .map(period -> period.getID_Defense())
                .distinct()
                .toList();
        if (periodIds.isEmpty()) return;
        members.findReviewerCommitteesByPeriods(
                        periodIds, CommitteeMemberRoleConstain.REVIEWER, CommitteeMemberStatusConstain.ACTIVE)
                .forEach(this::provisionCommittee);
    }

    private void provisionCommittee(DefenseCommitteesEntity committee) {
        if (committee.getStatus() == DefenseCommitteeStatusConstain.INACTIVE
                || committee.getDefensePeriod() == null
                || committee.getDefensePeriod().isFinished()) return;
        var reviewers = members.findByDefenseCommittees_IdComitteesAndRoleAndStatus(
                committee.getIdComittees(),
                CommitteeMemberRoleConstain.REVIEWER,
                CommitteeMemberStatusConstain.ACTIVE);
        if (reviewers.isEmpty()) return;

        var representative = reviewers.get(0).getLecture();
        LocalDateTime deadline = committee.getDefensePeriod().getEndDate() == null
                ? LocalDateTime.now(DefenseReportService.ZONE).plusYears(1)
                : committee.getDefensePeriod().getEndDate().atTime(LocalTime.MAX);
        for (TopicEntity topic : topics.findReviewableByPeriod(
                committee.getDefensePeriod().getID_Defense(), REVIEWABLE_STATUSES)) {
            if (assignments.existsByTopic_IdTopicAndDefenseCommittee_IdComitteesAndStatusNot(
                    topic.getIdTopic(), committee.getIdComittees(), ReviewAssignmentStatusConstain.CANCELLED)) {
                continue;
            }
            assignments.save(ReviewAssignmentEntity.builder()
                    .topic(topic)
                    .lecture(representative)
                    .defenseCommittee(committee)
                    .deadline(deadline)
                    .status(ReviewAssignmentStatusConstain.ASSIGNED)
                    .assignedAt(LocalDateTime.now(DefenseReportService.ZONE))
                    .note("Tự động tạo theo vai trò phản biện trong hội đồng")
                    .build());
        }
    }
}

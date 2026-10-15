package com.graduration.Service.ManagerService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.graduration.Constain.CommitteeMemberRoleConstain;
import com.graduration.Constain.CommitteeMemberStatusConstain;
import com.graduration.Constain.DefenseCommitteeStatusConstain;
import com.graduration.Constain.ReviewAssignmentStatusConstain;
import com.graduration.Repository.CommitteeMemberRepository;
import com.graduration.Repository.DefenseCommitteeRepository;
import com.graduration.Repository.ReviewAssignmentRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.entity.ComitteesMemberEntity;
import com.graduration.entity.DefenseCommitteesEntity;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.LectureEntity;
import com.graduration.entity.TeamEntity;
import com.graduration.entity.TopicEntity;

class DefenseReportProvisioningServiceTest {
    @Test
    void reviewerMembershipAutomaticallyCreatesNotSubmittedRowsForPeriodTopics() {
        DefenseCommitteeRepository committees = mock(DefenseCommitteeRepository.class);
        CommitteeMemberRepository members = mock(CommitteeMemberRepository.class);
        TopicRepository topics = mock(TopicRepository.class);
        ReviewAssignmentRepository assignments = mock(ReviewAssignmentRepository.class);
        var service = new ReviewAssignmentProvisioningService(committees, members, topics, assignments);

        var period = new DefensePeriodEntity();
        period.setID_Defense(10L);
        period.setStartDate(LocalDate.now().minusDays(1));
        period.setEndDate(LocalDate.now().plusDays(10));
        var committee = new DefenseCommitteesEntity();
        committee.setIdComittees(20L);
        committee.setStatus(DefenseCommitteeStatusConstain.DRAFT);
        committee.setDefensePeriod(period);
        var lecturer = LectureEntity.builder().lectureId("reviewer-1").build();
        var reviewer = ComitteesMemberEntity.builder()
                .lecture(lecturer)
                .role(CommitteeMemberRoleConstain.REVIEWER)
                .status(CommitteeMemberStatusConstain.ACTIVE)
                .build();
        var topic = TopicEntity.builder()
                .idTopic(30L)
                .team(TeamEntity.builder().idTeam(40L).build())
                .defensePeriod(period)
                .build();

        when(committees.findById(20L)).thenReturn(Optional.of(committee));
        when(members.findByDefenseCommittees_IdComitteesAndRoleAndStatus(
                        20L, CommitteeMemberRoleConstain.REVIEWER, CommitteeMemberStatusConstain.ACTIVE))
                .thenReturn(List.of(reviewer));
        when(topics.findReviewableByPeriod(eq(10L), anyCollection())).thenReturn(List.of(topic));
        when(assignments.existsByTopic_IdTopicAndDefenseCommittee_IdComitteesAndStatusNot(
                        30L, 20L, ReviewAssignmentStatusConstain.CANCELLED))
                .thenReturn(false);

        service.provisionCommittee(20L);

        ArgumentCaptor<com.graduration.entity.ReviewAssignmentEntity> captor =
                ArgumentCaptor.forClass(com.graduration.entity.ReviewAssignmentEntity.class);
        verify(assignments).save(captor.capture());
        var created = captor.getValue();
        assertEquals(ReviewAssignmentStatusConstain.ASSIGNED, created.getStatus());
        assertEquals(lecturer, created.getLecture());
        assertEquals(committee, created.getDefenseCommittee());
        assertEquals(period.getEndDate(), created.getDeadline().toLocalDate());
    }

    @Test
    void provisioningIsIdempotentForTheSameTopicAndCommittee() {
        DefenseCommitteeRepository committees = mock(DefenseCommitteeRepository.class);
        CommitteeMemberRepository members = mock(CommitteeMemberRepository.class);
        TopicRepository topics = mock(TopicRepository.class);
        ReviewAssignmentRepository assignments = mock(ReviewAssignmentRepository.class);
        var service = new ReviewAssignmentProvisioningService(committees, members, topics, assignments);
        var period = new DefensePeriodEntity();
        period.setID_Defense(10L);
        period.setStartDate(LocalDate.now().minusDays(1));
        period.setEndDate(LocalDate.now().plusDays(10));
        var committee = new DefenseCommitteesEntity();
        committee.setIdComittees(20L);
        committee.setStatus(DefenseCommitteeStatusConstain.ACTIVE);
        committee.setDefensePeriod(period);
        var reviewer = ComitteesMemberEntity.builder()
                .lecture(LectureEntity.builder().lectureId("reviewer-1").build())
                .role(CommitteeMemberRoleConstain.REVIEWER)
                .status(CommitteeMemberStatusConstain.ACTIVE)
                .build();
        var topic = TopicEntity.builder().idTopic(30L).defensePeriod(period).build();
        when(committees.findById(20L)).thenReturn(Optional.of(committee));
        when(members.findByDefenseCommittees_IdComitteesAndRoleAndStatus(anyLong(), any(), any()))
                .thenReturn(List.of(reviewer));
        when(topics.findReviewableByPeriod(eq(10L), anyCollection())).thenReturn(List.of(topic));
        when(assignments.existsByTopic_IdTopicAndDefenseCommittee_IdComitteesAndStatusNot(anyLong(), anyLong(), any()))
                .thenReturn(true);

        service.provisionCommittee(20L);

        verify(assignments, never()).save(any());
    }
}

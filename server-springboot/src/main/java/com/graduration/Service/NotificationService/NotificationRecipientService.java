package com.graduration.Service.NotificationService;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import com.graduration.Constain.CommitteeMemberStatusConstain;
import com.graduration.Constain.EnrollmentStatusConstain;
import com.graduration.Constain.SupervisorAssignmentStatusConstain;
import com.graduration.Repository.CommitteeMemberRepository;
import com.graduration.Repository.GraduationEnrollmentRepository;
import com.graduration.Repository.TeamRepository;
import com.graduration.Repository.TopicSupervisorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationRecipientService {
    private static final Logger log = LoggerFactory.getLogger(NotificationRecipientService.class);
    private final TeamRepository teamRepository;
    private final GraduationEnrollmentRepository graduationEnrollmentRepository;
    private final TopicSupervisorRepository topicSupervisorRepository;
    private final CommitteeMemberRepository committeeMemberRepository;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Set<String> studentUserIdsForTeam(Long teamId) {
        if (teamId == null) return Set.of();
        return safely(() -> teamRepository.findWithDetailsByIdTeam(teamId)
                .map(team -> team.getStudentEntities().stream()
                        .filter(student -> student.getUserEntity() != null)
                        .map(student -> student.getUserEntity().getUserId())
                        .filter(value -> value != null && !value.isBlank())
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)))
                .orElseGet(LinkedHashSet::new));
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Set<String> studentUserIdsForTopic(Long topicId) {
        if (topicId == null) return Set.of();
        return safely(() -> teamRepository.findByTopic_IdTopic(topicId)
                .map(team -> team.getStudentEntities().stream()
                        .filter(student -> student.getUserEntity() != null)
                        .map(student -> student.getUserEntity().getUserId())
                        .filter(value -> value != null && !value.isBlank())
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)))
                .orElseGet(LinkedHashSet::new));
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Set<String> studentUserIdsForDefensePeriod(Long defensePeriodId) {
        Set<String> recipients = new LinkedHashSet<>();
        if (defensePeriodId == null) return recipients;
        return safely(() -> {
            teamRepository.findAllForNotificationsByDefensePeriod(defensePeriodId).forEach(team -> team.getStudentEntities()
                    .stream()
                    .filter(student -> student.getUserEntity() != null)
                    .map(student -> student.getUserEntity().getUserId())
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(recipients::add));
            graduationEnrollmentRepository
                    .findForNotificationsByDefensePeriod(defensePeriodId, EnrollmentStatusConstain.WITHDRAWN)
                    .stream()
                    .filter(enrollment -> enrollment.getStudent() != null
                            && enrollment.getStudent().getUserEntity() != null)
                    .map(enrollment -> enrollment.getStudent().getUserEntity().getUserId())
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(recipients::add);
            return recipients;
        });
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Set<String> activeSupervisorUserIds(Long topicId) {
        Set<String> recipients = new LinkedHashSet<>();
        if (topicId == null) return recipients;
        return safely(() -> {
            topicSupervisorRepository
                    .findByTopic_IdTopicAndStatus(topicId, SupervisorAssignmentStatusConstain.ACTIVE)
                    .stream()
                    .filter(assignment -> assignment.getLecture() != null && assignment.getLecture().getUser() != null)
                    .map(assignment -> assignment.getLecture().getUser().getUserId())
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(recipients::add);
            return recipients;
        });
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Set<String> activeCommitteeUserIds(Long committeeId) {
        Set<String> recipients = new LinkedHashSet<>();
        if (committeeId == null) return recipients;
        return safely(() -> {
            committeeMemberRepository
                    .findByDefenseCommittees_IdComitteesAndStatusOrderByRoleAsc(
                            committeeId, CommitteeMemberStatusConstain.ACTIVE)
                    .stream()
                    .filter(member -> member.getLecture() != null && member.getLecture().getUser() != null)
                    .map(member -> member.getLecture().getUser().getUserId())
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(recipients::add);
            return recipients;
        });
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Set<String> defenseScheduleRecipients(Long topicId, Long committeeId) {
        Set<String> recipients = new LinkedHashSet<>(studentUserIdsForTopic(topicId));
        recipients.addAll(activeSupervisorUserIds(topicId));
        recipients.addAll(activeCommitteeUserIds(committeeId));
        return recipients;
    }

    private Set<String> safely(Supplier<Set<String>> resolver) {
        try {
            return resolver.get();
        } catch (Exception exception) {
            log.warn("Không thể xác định người nhận thông báo: {}", exception.getMessage());
            return Set.of();
        }
    }
}

package com.graduration.Service.ManagerService;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.Constain.StatusConstain;
import com.graduration.Constain.SupervisorAssignmentStatusConstain;
import com.graduration.Constain.SupervisorRoleConstain;
import com.graduration.Constain.TopicStatusConstain;
import com.graduration.DTO.Request.AssignTopicSupervisorRequest;
import com.graduration.DTO.Request.DeactivateTopicSupervisorRequest;
import com.graduration.DTO.Request.UpdateTopicSupervisorRequest;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.TopicSupervisorResponse;
import com.graduration.Repository.LectureRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.Repository.TopicSupervisorRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.LectureEntity;
import com.graduration.entity.TopicEntity;
import com.graduration.entity.TopicSuperVisorEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.TopicSupervisorMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicSupervisorService {
    final TopicSupervisorRepository supervisorRepository;
    final TopicRepository topicRepository;
    final LectureRepository lectureRepository;
    final UserRepository userRepository;
    final TopicSupervisorMapper supervisorMapper;

    @Value("${app.supervision.max-topics-per-period:5}")
    int maxTopicsPerPeriod;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public TopicSupervisorResponse assign(Long topicId, AssignTopicSupervisorRequest request) {
        TopicEntity topic = findAssignableTopic(topicId);
        LectureEntity lecture = lectureRepository
                .findById(request.getLectureId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        requireActiveLecturer(lecture);
        if (supervisorRepository.existsByTopic_IdTopicAndLecture_LectureIdAndStatus(
                topicId, lecture.getLectureId(), SupervisorAssignmentStatusConstain.ACTIVE)) {
            throw new AppException(ErrorCode.TOPIC_SUPERVISOR_ALREADY_ASSIGNED);
        }
        requirePrimaryAvailable(topicId, request.getRole(), null);
        long assignments = supervisorRepository.countActiveAssignments(
                lecture.getLectureId(),
                topic.getDefensePeriod().getID_Defense(),
                SupervisorAssignmentStatusConstain.ACTIVE);
        if (assignments >= maxTopicsPerPeriod) {
            throw new AppException(ErrorCode.TOPIC_SUPERVISOR_LIMIT_REACHED);
        }
        TopicSuperVisorEntity assignment = TopicSuperVisorEntity.builder()
                .topic(topic)
                .lecture(lecture)
                .supervisorRole(request.getRole())
                .status(SupervisorAssignmentStatusConstain.ACTIVE)
                .assignedAt(LocalDateTime.now())
                .assignedBy(currentUser())
                .note(normalize(request.getNote()))
                .build();
        return supervisorMapper.toResponse(supervisorRepository.save(assignment));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public PageResponse<TopicSupervisorResponse> getByTopic(Long topicId, Integer page, Integer size) {
        if (!topicRepository.existsById(topicId)) {
            throw new AppException(ErrorCode.TOPIC_NOT_FOUND);
        }
        return PageResponse.from(
                supervisorRepository.findByTopic_IdTopic(topicId, PaginationSupport.pageRequest(page, size)),
                supervisorMapper::toResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    public PageResponse<TopicSupervisorResponse> getByLecturer(String lectureId, Integer page, Integer size) {
        if (!lectureRepository.existsById(lectureId)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return activeByLecturer(lectureId, page, size);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR', 'ROLE_FACULTY', 'ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public PageResponse<TopicSupervisorResponse> getMine(Integer page, Integer size) {
        LectureEntity lecture = lectureRepository
                .findByUser_UserId(currentAuthentication().getName())
                .orElseThrow(() -> new AppException(ErrorCode.LECTURER_PROFILE_NOT_FOUND));
        return activeByLecturer(lecture.getLectureId(), page, size);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public TopicSupervisorResponse update(Long assignmentId, UpdateTopicSupervisorRequest request) {
        TopicSuperVisorEntity assignment = findAssignment(assignmentId);
        requireActive(assignment);
        findAssignableTopic(assignment.getTopic().getIdTopic());
        requirePrimaryAvailable(assignment.getTopic().getIdTopic(), request.getRole(), assignmentId);
        assignment.setSupervisorRole(request.getRole());
        assignment.setNote(normalize(request.getNote()));
        return supervisorMapper.toResponse(supervisorRepository.save(assignment));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public TopicSupervisorResponse deactivate(Long assignmentId, DeactivateTopicSupervisorRequest request) {
        TopicSuperVisorEntity assignment = findAssignment(assignmentId);
        requireActive(assignment);
        if (request == null
                || request.getReason() == null
                || request.getReason().isBlank()) {
            throw new AppException(ErrorCode.SUPERVISOR_DEACTIVATION_REASON_NOT_BLANK);
        }
        assignment.setStatus(SupervisorAssignmentStatusConstain.INACTIVE);
        assignment.setEndedAt(LocalDateTime.now());
        assignment.setNote(appendReason(assignment.getNote(), request.getReason()));
        return supervisorMapper.toResponse(supervisorRepository.save(assignment));
    }

    private PageResponse<TopicSupervisorResponse> activeByLecturer(String lectureId, Integer page, Integer size) {
        return PageResponse.from(
                supervisorRepository.findByLecture_LectureIdAndStatus(
                        lectureId,
                        SupervisorAssignmentStatusConstain.ACTIVE,
                        PaginationSupport.pageRequest(page, size)),
                supervisorMapper::toResponse);
    }

    private TopicEntity findAssignableTopic(Long topicId) {
        TopicEntity topic =
                topicRepository.findById(topicId).orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
        if (topic.getDefensePeriod() == null
                || topic.getDefensePeriod().getStatus() == DefensePeriodConstain.FINISHED) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_FINISHED);
        }
        if (topic.getStatus() == TopicStatusConstain.REJECTED || topic.getStatus() == TopicStatusConstain.CANCELLED) {
            throw new AppException(ErrorCode.TOPIC_OPERATION_NOT_ALLOWED);
        }
        return topic;
    }

    private void requireActiveLecturer(LectureEntity lecture) {
        StatusConstain status =
                lecture.getUser() == null ? null : lecture.getUser().getStatus();
        if (lecture.getUser() == null || status == StatusConstain.INACTIVE || status == StatusConstain.DELETED) {
            throw new AppException(ErrorCode.LECTURER_INACTIVE);
        }
    }

    private void requirePrimaryAvailable(Long topicId, SupervisorRoleConstain role, Long excludedAssignmentId) {
        if (role != SupervisorRoleConstain.PRIMARY) {
            return;
        }
        boolean exists = excludedAssignmentId == null
                ? supervisorRepository.existsByTopic_IdTopicAndSupervisorRoleAndStatus(
                        topicId, SupervisorRoleConstain.PRIMARY, SupervisorAssignmentStatusConstain.ACTIVE)
                : supervisorRepository.existsByTopic_IdTopicAndSupervisorRoleAndStatusAndIdSuperVisorNot(
                        topicId,
                        SupervisorRoleConstain.PRIMARY,
                        SupervisorAssignmentStatusConstain.ACTIVE,
                        excludedAssignmentId);
        if (exists) {
            throw new AppException(ErrorCode.TOPIC_PRIMARY_SUPERVISOR_ALREADY_EXISTS);
        }
    }

    private TopicSuperVisorEntity findAssignment(Long assignmentId) {
        return supervisorRepository
                .findById(assignmentId)
                .orElseThrow(() -> new AppException(ErrorCode.TOPIC_SUPERVISOR_NOT_FOUND));
    }

    private void requireActive(TopicSuperVisorEntity assignment) {
        if (assignment.getStatus() != SupervisorAssignmentStatusConstain.ACTIVE) {
            throw new AppException(ErrorCode.SUPERVISOR_ASSIGNMENT_NOT_ACTIVE);
        }
    }

    private UserEntity currentUser() {
        return userRepository
                .findById(currentAuthentication().getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Authentication currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String appendReason(String note, String reason) {
        String normalizedReason = reason.trim();
        return note == null || note.isBlank()
                ? "Deactivated: " + normalizedReason
                : note.trim() + " | Deactivated: " + normalizedReason;
    }
}

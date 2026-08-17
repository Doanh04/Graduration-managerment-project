package com.graduration.Service.ManagerService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.CommitteeMemberRoleConstain;
import com.graduration.Constain.CommitteeMemberStatusConstain;
import com.graduration.Constain.DefenseCommitteeStatusConstain;
import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.Constain.DefenseScheduleConflictTypeConstain;
import com.graduration.Constain.DefenseScheduleHistoryActionConstain;
import com.graduration.Constain.DefenseScheduleStatusConstain;
import com.graduration.Constain.SupervisorAssignmentStatusConstain;
import com.graduration.Constain.TopicStatusConstain;
import com.graduration.DTO.Request.DefenseScheduleRequest;
import com.graduration.DTO.Request.RescheduleDefenseRequest;
import com.graduration.DTO.Request.ScheduleReasonRequest;
import com.graduration.DTO.Response.DefenseScheduleResponse;
import com.graduration.DTO.Response.DefenseScheduleValidationResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.Repository.DefenseCommitteeRepository;
import com.graduration.Repository.DefensePeriodRepository;
import com.graduration.Repository.DefenseScheduleRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.Service.ManagerService.DefenseScheduleHistoryService.Snapshot;
import com.graduration.entity.DefenseCommitteesEntity;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.DefenseSchedulesEntity;
import com.graduration.entity.TopicEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.DefenseScheduleMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefenseScheduleService {
    DefenseScheduleRepository scheduleRepository;
    DefensePeriodRepository periodRepository;
    DefenseCommitteeRepository committeeRepository;
    TopicRepository topicRepository;
    UserRepository userRepository;
    DefenseScheduleMapper scheduleMapper;
    DefenseScheduleHistoryService historyService;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public DefenseScheduleResponse create(Long periodId, DefenseScheduleRequest request) {
        DefensePeriodEntity period = findOpenPeriod(periodId);
        TopicEntity topic = findTopic(request.getTopicId());
        DefenseCommitteesEntity committee = findCommittee(request.getCommitteeId());
        DefenseScheduleValidationResponse validation = validateInternal(period, topic, committee, request, null);
        requireValid(validation);
        UserEntity actor = currentUser();
        DefenseSchedulesEntity schedule = DefenseSchedulesEntity.builder()
                .topic(topic)
                .defenseCommittees(committee)
                .status(DefenseScheduleStatusConstain.DRAFT)
                .createdBy(actor)
                .build();
        apply(schedule, request);
        DefenseSchedulesEntity saved = scheduleRepository.save(schedule);
        historyService.record(
                saved, DefenseScheduleHistoryActionConstain.CREATED, null, historyService.snapshot(saved), null, actor);
        return scheduleMapper.toResponse(saved);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    public PageResponse<DefenseScheduleResponse> getByPeriod(
            Long periodId,
            LocalDate date,
            Long committeeId,
            String room,
            DefenseScheduleStatusConstain status,
            Integer page,
            Integer size) {
        if (!periodRepository.existsById(periodId)) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND);
        }
        return PageResponse.from(
                scheduleRepository.findByPeriod(
                        periodId,
                        date,
                        committeeId,
                        normalize(room),
                        status,
                        PaginationSupport.pageRequest(page, size)),
                scheduleMapper::toResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    public DefenseScheduleResponse getById(Long scheduleId) {
        return scheduleMapper.toResponse(findSchedule(scheduleId));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    public DefenseScheduleValidationResponse validate(Long periodId, DefenseScheduleRequest request) {
        return validateInternal(
                findOpenPeriod(periodId),
                findTopic(request.getTopicId()),
                findCommittee(request.getCommitteeId()),
                request,
                null);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public DefenseScheduleResponse update(Long scheduleId, DefenseScheduleRequest request) {
        DefenseSchedulesEntity schedule = findSchedule(scheduleId);
        requireStatus(schedule, DefenseScheduleStatusConstain.DRAFT);
        Snapshot before = historyService.snapshot(schedule);
        DefensePeriodEntity period =
                findOpenPeriod(schedule.getTopic().getDefensePeriod().getID_Defense());
        TopicEntity topic = findTopic(request.getTopicId());
        DefenseCommitteesEntity committee = findCommittee(request.getCommitteeId());
        DefenseScheduleValidationResponse validation = validateInternal(period, topic, committee, request, scheduleId);
        requireValid(validation);
        schedule.setTopic(topic);
        schedule.setDefenseCommittees(committee);
        apply(schedule, request);
        DefenseSchedulesEntity saved = scheduleRepository.save(schedule);
        historyService.record(
                saved,
                DefenseScheduleHistoryActionConstain.UPDATED,
                before,
                historyService.snapshot(saved),
                null,
                currentUser());
        return scheduleMapper.toResponse(saved);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public DefenseScheduleResponse publish(Long scheduleId) {
        DefenseSchedulesEntity schedule = findSchedule(scheduleId);
        requireStatus(schedule, DefenseScheduleStatusConstain.DRAFT);
        Snapshot before = historyService.snapshot(schedule);
        DefensePeriodEntity period =
                findOpenPeriod(schedule.getTopic().getDefensePeriod().getID_Defense());
        DefenseScheduleRequest request = fromEntity(schedule);
        requireValid(
                validateInternal(period, schedule.getTopic(), schedule.getDefenseCommittees(), request, scheduleId));
        schedule.setStatus(DefenseScheduleStatusConstain.PUBLISHED);
        schedule.setPublishedAt(LocalDateTime.now());
        DefenseSchedulesEntity saved = scheduleRepository.save(schedule);
        historyService.record(
                saved,
                DefenseScheduleHistoryActionConstain.PUBLISHED,
                before,
                historyService.snapshot(saved),
                null,
                currentUser());
        return scheduleMapper.toResponse(saved);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public DefenseScheduleResponse postpone(Long scheduleId, ScheduleReasonRequest request) {
        DefenseSchedulesEntity schedule = findSchedule(scheduleId);
        if (schedule.getStatus() != DefenseScheduleStatusConstain.PUBLISHED
                && schedule.getStatus() != DefenseScheduleStatusConstain.SCHEDULED) {
            throw new AppException(ErrorCode.DEFENSE_SCHEDULE_OPERATION_NOT_ALLOWED);
        }
        Snapshot before = historyService.snapshot(schedule);
        String reason = requireReason(request);
        schedule.setStatus(DefenseScheduleStatusConstain.POSTPONED);
        schedule.setPostponedAt(LocalDateTime.now());
        schedule.setPostponedReason(reason);
        DefenseSchedulesEntity saved = scheduleRepository.save(schedule);
        historyService.record(
                saved,
                DefenseScheduleHistoryActionConstain.POSTPONED,
                before,
                historyService.snapshot(saved),
                reason,
                currentUser());
        return scheduleMapper.toResponse(saved);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public DefenseScheduleResponse reschedule(Long scheduleId, RescheduleDefenseRequest request) {
        DefenseSchedulesEntity schedule = findSchedule(scheduleId);
        requireStatus(schedule, DefenseScheduleStatusConstain.POSTPONED);
        Snapshot before = historyService.snapshot(schedule);
        if (request == null
                || request.getReason() == null
                || request.getReason().isBlank()) {
            throw new AppException(ErrorCode.DEFENSE_SCHEDULE_REASON_NOT_BLANK);
        }
        DefensePeriodEntity period =
                findOpenPeriod(schedule.getTopic().getDefensePeriod().getID_Defense());
        DefenseCommitteesEntity committee = findCommittee(request.getCommitteeId());
        DefenseScheduleRequest candidate = DefenseScheduleRequest.builder()
                .topicId(schedule.getTopic().getIdTopic())
                .committeeId(request.getCommitteeId())
                .defenseDate(request.getDefenseDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .room(request.getRoom())
                .location(request.getLocation())
                .session(request.getSession())
                .note(schedule.getNote())
                .build();
        requireValid(validateInternal(period, schedule.getTopic(), committee, candidate, scheduleId));
        schedule.setDefenseCommittees(committee);
        apply(schedule, candidate);
        schedule.setStatus(DefenseScheduleStatusConstain.DRAFT);
        schedule.setPostponedReason(schedule.getPostponedReason() + " | Rescheduled: "
                + request.getReason().trim());
        DefenseSchedulesEntity saved = scheduleRepository.save(schedule);
        historyService.record(
                saved,
                DefenseScheduleHistoryActionConstain.RESCHEDULED,
                before,
                historyService.snapshot(saved),
                request.getReason(),
                currentUser());
        return scheduleMapper.toResponse(saved);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public DefenseScheduleResponse complete(Long scheduleId) {
        DefenseSchedulesEntity schedule = findSchedule(scheduleId);
        if (schedule.getStatus() != DefenseScheduleStatusConstain.PUBLISHED
                && schedule.getStatus() != DefenseScheduleStatusConstain.SCHEDULED) {
            throw new AppException(ErrorCode.DEFENSE_SCHEDULE_OPERATION_NOT_ALLOWED);
        }
        if (LocalDateTime.of(schedule.getDefenseDate(), schedule.getEndTime()).isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.DEFENSE_SCHEDULE_NOT_FINISHED);
        }
        Snapshot before = historyService.snapshot(schedule);
        schedule.setStatus(DefenseScheduleStatusConstain.COMPLETED);
        schedule.getTopic().setStatus(TopicStatusConstain.COMPLETED);
        DefenseSchedulesEntity saved = scheduleRepository.save(schedule);
        historyService.record(
                saved,
                DefenseScheduleHistoryActionConstain.COMPLETED,
                before,
                historyService.snapshot(saved),
                null,
                currentUser());
        return scheduleMapper.toResponse(saved);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public DefenseScheduleResponse cancel(Long scheduleId, ScheduleReasonRequest request) {
        DefenseSchedulesEntity schedule = findSchedule(scheduleId);
        if (schedule.getStatus() == DefenseScheduleStatusConstain.COMPLETED
                || schedule.getStatus() == DefenseScheduleStatusConstain.CANCELLED) {
            throw new AppException(ErrorCode.DEFENSE_SCHEDULE_OPERATION_NOT_ALLOWED);
        }
        Snapshot before = historyService.snapshot(schedule);
        String reason = requireReason(request);
        schedule.setStatus(DefenseScheduleStatusConstain.CANCELLED);
        schedule.setCancelledAt(LocalDateTime.now());
        schedule.setCancelledReason(reason);
        DefenseSchedulesEntity saved = scheduleRepository.save(schedule);
        historyService.record(
                saved,
                DefenseScheduleHistoryActionConstain.CANCELLED,
                before,
                historyService.snapshot(saved),
                reason,
                currentUser());
        return scheduleMapper.toResponse(saved);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public void delete(Long scheduleId) {
        DefenseSchedulesEntity schedule = findSchedule(scheduleId);
        requireStatus(schedule, DefenseScheduleStatusConstain.DRAFT);
        if (historyService.hasHistory(scheduleId)) {
            throw new AppException(ErrorCode.DEFENSE_SCHEDULE_OPERATION_NOT_ALLOWED);
        }
        scheduleRepository.delete(schedule);
    }

    private DefenseScheduleValidationResponse validateInternal(
            DefensePeriodEntity period,
            TopicEntity topic,
            DefenseCommitteesEntity committee,
            DefenseScheduleRequest request,
            Long excludedScheduleId) {
        List<DefenseScheduleValidationResponse.Conflict> conflicts = new ArrayList<>();
        if (request == null) {
            add(conflicts, DefenseScheduleConflictTypeConstain.INVALID_TIME, "Schedule request is required");
            return response(conflicts);
        }
        if (request.getDefenseDate() == null
                || request.getStartTime() == null
                || request.getEndTime() == null
                || !request.getStartTime().isBefore(request.getEndTime())) {
            add(conflicts, DefenseScheduleConflictTypeConstain.INVALID_TIME, "Start time must be before end time");
            return response(conflicts);
        }
        if (request.getRoom() == null
                || request.getRoom().isBlank()
                || request.getLocation() == null
                || request.getLocation().isBlank()) {
            add(conflicts, DefenseScheduleConflictTypeConstain.INVALID_TIME, "Room and location are required");
        }
        Long periodId = period.getID_Defense();
        if (topic.getDefensePeriod() == null
                || committee.getDefensePeriod() == null
                || !periodId.equals(topic.getDefensePeriod().getID_Defense())
                || !periodId.equals(committee.getDefensePeriod().getID_Defense())) {
            add(
                    conflicts,
                    DefenseScheduleConflictTypeConstain.PERIOD_MISMATCH,
                    "Topic and committee must belong to the selected defense period");
        }
        if (period.getStartDate() != null
                && period.getEndDate() != null
                && (request.getDefenseDate().isBefore(period.getStartDate())
                        || request.getDefenseDate().isAfter(period.getEndDate()))) {
            add(
                    conflicts,
                    DefenseScheduleConflictTypeConstain.PERIOD_MISMATCH,
                    "Defense date must be inside the defense period");
        }
        if (topic.getTeam() == null
                || (topic.getStatus() != TopicStatusConstain.REGISTERED
                        && topic.getStatus() != TopicStatusConstain.IN_PROGRESS)) {
            add(
                    conflicts,
                    DefenseScheduleConflictTypeConstain.TOPIC_NOT_ELIGIBLE,
                    "Topic must have a team and be registered or in progress");
        }
        if (topic.getDefenseSchedule() != null
                && (excludedScheduleId == null
                        || !excludedScheduleId.equals(topic.getDefenseSchedule().getIdDefenseScheduce()))) {
            add(
                    conflicts,
                    DefenseScheduleConflictTypeConstain.TOPIC_ALREADY_SCHEDULED,
                    "Topic already has a defense schedule");
        }
        if (committee.getStatus() != DefenseCommitteeStatusConstain.ACTIVE) {
            add(
                    conflicts,
                    DefenseScheduleConflictTypeConstain.COMMITTEE_NOT_ACTIVE,
                    "Defense committee must be active");
        }
        if (request.getRoom() != null
                && scheduleRepository.hasRoomConflict(
                        request.getDefenseDate(),
                        request.getRoom().trim(),
                        request.getStartTime(),
                        request.getEndTime(),
                        DefenseScheduleStatusConstain.CANCELLED,
                        excludedScheduleId)) {
            add(
                    conflicts,
                    DefenseScheduleConflictTypeConstain.ROOM_CONFLICT,
                    "Another defense schedule uses this room during the selected time");
        }
        if (scheduleRepository.hasCommitteeConflict(
                request.getDefenseDate(),
                committee.getIdComittees(),
                request.getStartTime(),
                request.getEndTime(),
                DefenseScheduleStatusConstain.CANCELLED,
                excludedScheduleId)) {
            add(
                    conflicts,
                    DefenseScheduleConflictTypeConstain.COMMITTEE_CONFLICT,
                    "Committee already has another defense during the selected time");
        }
        if (scheduleRepository.hasLecturerConflict(
                request.getDefenseDate(),
                committee.getIdComittees(),
                request.getStartTime(),
                request.getEndTime(),
                CommitteeMemberStatusConstain.ACTIVE,
                DefenseScheduleStatusConstain.CANCELLED,
                excludedScheduleId)) {
            add(
                    conflicts,
                    DefenseScheduleConflictTypeConstain.LECTURER_CONFLICT,
                    "At least one active committee member has another defense during the selected time");
        }
        if (hasSupervisorReviewerConflict(topic, committee)) {
            add(
                    conflicts,
                    DefenseScheduleConflictTypeConstain.SUPERVISOR_REVIEWER_CONFLICT,
                    "A topic supervisor cannot be a reviewer for the same topic");
        }
        return response(conflicts);
    }

    private boolean hasSupervisorReviewerConflict(TopicEntity topic, DefenseCommitteesEntity committee) {
        Set<String> supervisors = topic.getTopicSuperVisorEntities().stream()
                .filter(item -> item.getStatus() == SupervisorAssignmentStatusConstain.ACTIVE)
                .filter(item -> item.getLecture() != null)
                .map(item -> item.getLecture().getLectureId())
                .collect(Collectors.toSet());
        return committee.getComitteesMember().stream()
                .filter(item -> item.getStatus() == CommitteeMemberStatusConstain.ACTIVE)
                .filter(item -> item.getRole() == CommitteeMemberRoleConstain.REVIEWER)
                .anyMatch(item -> item.getLecture() != null
                        && supervisors.contains(item.getLecture().getLectureId()));
    }

    private void apply(DefenseSchedulesEntity schedule, DefenseScheduleRequest request) {
        schedule.setDefenseDate(request.getDefenseDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setRoom(request.getRoom().trim());
        schedule.setLocation(request.getLocation().trim());
        schedule.setSession(request.getSession());
        schedule.setNote(normalize(request.getNote()));
    }

    private DefenseScheduleRequest fromEntity(DefenseSchedulesEntity schedule) {
        return DefenseScheduleRequest.builder()
                .topicId(schedule.getTopic().getIdTopic())
                .committeeId(schedule.getDefenseCommittees().getIdComittees())
                .defenseDate(schedule.getDefenseDate())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .room(schedule.getRoom())
                .location(schedule.getLocation())
                .session(schedule.getSession())
                .note(schedule.getNote())
                .build();
    }

    private void requireValid(DefenseScheduleValidationResponse validation) {
        if (!validation.isValid()) {
            throw new AppException(ErrorCode.DEFENSE_SCHEDULE_CONFLICT);
        }
    }

    private DefenseScheduleValidationResponse response(List<DefenseScheduleValidationResponse.Conflict> conflicts) {
        return DefenseScheduleValidationResponse.builder()
                .valid(conflicts.isEmpty())
                .conflicts(conflicts)
                .build();
    }

    private void add(
            List<DefenseScheduleValidationResponse.Conflict> conflicts,
            DefenseScheduleConflictTypeConstain type,
            String message) {
        conflicts.add(DefenseScheduleValidationResponse.Conflict.builder()
                .type(type)
                .message(message)
                .build());
    }

    private DefenseSchedulesEntity findSchedule(Long scheduleId) {
        return scheduleRepository
                .findById(scheduleId)
                .orElseThrow(() -> new AppException(ErrorCode.DEFENSE_SCHEDULE_NOT_FOUND));
    }

    private TopicEntity findTopic(Long topicId) {
        return topicRepository.findById(topicId).orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
    }

    private DefenseCommitteesEntity findCommittee(Long committeeId) {
        return committeeRepository
                .findById(committeeId)
                .orElseThrow(() -> new AppException(ErrorCode.DEFENSE_COMMITTEE_NOT_FOUND));
    }

    private DefensePeriodEntity findOpenPeriod(Long periodId) {
        DefensePeriodEntity period = periodRepository
                .findById(periodId)
                .orElseThrow(() -> new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND));
        if (period.getStatus() == DefensePeriodConstain.FINISHED) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_FINISHED);
        }
        return period;
    }

    private void requireStatus(DefenseSchedulesEntity schedule, DefenseScheduleStatusConstain status) {
        if (schedule.getStatus() != status) {
            throw new AppException(ErrorCode.DEFENSE_SCHEDULE_OPERATION_NOT_ALLOWED);
        }
    }

    private String requireReason(ScheduleReasonRequest request) {
        if (request == null
                || request.getReason() == null
                || request.getReason().isBlank()) {
            throw new AppException(ErrorCode.DEFENSE_SCHEDULE_REASON_NOT_BLANK);
        }
        return request.getReason().trim();
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
}

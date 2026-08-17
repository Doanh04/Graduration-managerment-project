package com.graduration.Service.ManagerService;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.DefenseScheduleHistoryActionConstain;
import com.graduration.Constain.DefenseScheduleStatusConstain;
import com.graduration.DTO.Response.DefenseScheduleHistoryResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.Repository.DefenseScheduleHistoryRepository;
import com.graduration.Repository.DefenseScheduleRepository;
import com.graduration.entity.DefenseScheduleHistoryEntity;
import com.graduration.entity.DefenseSchedulesEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefenseScheduleHistoryService {
    DefenseScheduleHistoryRepository historyRepository;
    DefenseScheduleRepository scheduleRepository;

    @Transactional
    public void record(
            DefenseSchedulesEntity schedule,
            DefenseScheduleHistoryActionConstain action,
            Snapshot before,
            Snapshot after,
            String reason,
            UserEntity changedBy) {
        DefenseScheduleHistoryEntity history = DefenseScheduleHistoryEntity.builder()
                .schedule(schedule)
                .action(action)
                .previousStatus(before == null ? null : before.status())
                .newStatus(after == null ? null : after.status())
                .oldDefenseDate(before == null ? null : before.defenseDate())
                .oldStartTime(before == null ? null : before.startTime())
                .oldEndTime(before == null ? null : before.endTime())
                .oldRoom(before == null ? null : before.room())
                .oldLocation(before == null ? null : before.location())
                .oldCommitteeId(before == null ? null : before.committeeId())
                .oldCommitteeName(before == null ? null : before.committeeName())
                .newDefenseDate(after == null ? null : after.defenseDate())
                .newStartTime(after == null ? null : after.startTime())
                .newEndTime(after == null ? null : after.endTime())
                .newRoom(after == null ? null : after.room())
                .newLocation(after == null ? null : after.location())
                .newCommitteeId(after == null ? null : after.committeeId())
                .newCommitteeName(after == null ? null : after.committeeName())
                .reason(normalize(reason))
                .changedBy(changedBy)
                .build();
        historyRepository.save(history);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    public PageResponse<DefenseScheduleHistoryResponse> getHistory(Long scheduleId, Integer page, Integer size) {
        if (!scheduleRepository.existsById(scheduleId)) {
            throw new AppException(ErrorCode.DEFENSE_SCHEDULE_NOT_FOUND);
        }
        return PageResponse.from(
                historyRepository.findByScheduleId(scheduleId, PaginationSupport.pageRequest(page, size)),
                this::toResponse);
    }

    public boolean hasHistory(Long scheduleId) {
        return historyRepository.existsBySchedule_IdDefenseScheduce(scheduleId);
    }

    public Snapshot snapshot(DefenseSchedulesEntity schedule) {
        return new Snapshot(
                schedule.getDefenseDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getRoom(),
                schedule.getLocation(),
                schedule.getDefenseCommittees() == null
                        ? null
                        : schedule.getDefenseCommittees().getIdComittees(),
                schedule.getDefenseCommittees() == null
                        ? null
                        : schedule.getDefenseCommittees().getComitteesName(),
                schedule.getStatus());
    }

    private DefenseScheduleHistoryResponse toResponse(DefenseScheduleHistoryEntity history) {
        return DefenseScheduleHistoryResponse.builder()
                .historyId(history.getHistoryId())
                .scheduleId(history.getSchedule().getIdDefenseScheduce())
                .action(history.getAction())
                .previousStatus(history.getPreviousStatus())
                .newStatus(history.getNewStatus())
                .oldSchedule(toResponseSnapshot(
                        history.getOldDefenseDate(),
                        history.getOldStartTime(),
                        history.getOldEndTime(),
                        history.getOldRoom(),
                        history.getOldLocation(),
                        history.getOldCommitteeId(),
                        history.getOldCommitteeName()))
                .newSchedule(toResponseSnapshot(
                        history.getNewDefenseDate(),
                        history.getNewStartTime(),
                        history.getNewEndTime(),
                        history.getNewRoom(),
                        history.getNewLocation(),
                        history.getNewCommitteeId(),
                        history.getNewCommitteeName()))
                .reason(history.getReason())
                .changedByUserId(
                        history.getChangedBy() == null
                                ? null
                                : history.getChangedBy().getUserId())
                .changedByUsername(
                        history.getChangedBy() == null
                                ? null
                                : history.getChangedBy().getUserName())
                .changedAt(history.getChangedAt())
                .build();
    }

    private DefenseScheduleHistoryResponse.ScheduleSnapshot toResponseSnapshot(
            LocalDate date,
            LocalTime start,
            LocalTime end,
            String room,
            String location,
            Long committeeId,
            String committeeName) {
        if (date == null && start == null && end == null && room == null && committeeId == null) {
            return null;
        }
        return DefenseScheduleHistoryResponse.ScheduleSnapshot.builder()
                .defenseDate(date)
                .startTime(start)
                .endTime(end)
                .room(room)
                .location(location)
                .committeeId(committeeId)
                .committeeName(committeeName)
                .build();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record Snapshot(
            LocalDate defenseDate,
            LocalTime startTime,
            LocalTime endTime,
            String room,
            String location,
            Long committeeId,
            String committeeName,
            DefenseScheduleStatusConstain status) {}
}

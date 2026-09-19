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
    // Hàm record: Nhận lịch bảo vệ, thao tác, snapshot trước/sau, lý do và người thực hiện; sao chép toàn bộ thay đổi
    // trạng thái/lịch vào bản ghi lịch sử rồi lưu.
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
    // Hàm getHistory: Nhận mã hoặc điều kiện tìm kiếm của getHistory, truy vấn bản ghi/quan hệ tương ứng, báo lỗi khi
    // không tồn tại và trả về dữ liệu đã ánh xạ.
    public PageResponse<DefenseScheduleHistoryResponse> getHistory(Long scheduleId, Integer page, Integer size) {
        if (!scheduleRepository.existsById(scheduleId)) {
            throw new AppException(ErrorCode.DEFENSE_SCHEDULE_NOT_FOUND);
        }
        return PageResponse.from(
                historyRepository.findByScheduleId(scheduleId, PaginationSupport.pageRequest(page, size)),
                this::toResponse);
    }

    // Hàm hasHistory: Nhận mã lịch bảo vệ; kiểm tra repository có bản ghi lịch sử tương ứng để quyết định lịch có được
    // phép xóa hay không.
    public boolean hasHistory(Long scheduleId) {
        return historyRepository.existsBySchedule_IdDefenseScheduce(scheduleId);
    }

    /** Xóa các bản ghi lịch sử đi kèm lịch nháp trước khi xóa lịch để không vi phạm khóa ngoại. */
    @Transactional
    public void deleteHistory(Long scheduleId) {
        historyRepository.deleteBySchedule_IdDefenseScheduce(scheduleId);
        historyRepository.flush();
    }

    // Hàm snapshot: Nhận entity lịch bảo vệ; trích xuất ngày, giờ, phòng, địa điểm, hội đồng và trạng thái tại một thời
    // điểm để so sánh trước/sau.
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

    // Hàm toResponse: Nhận entity nghiệp vụ; lấy các quan hệ liên quan và ánh xạ mã, tên, trạng thái, thời gian cùng
    // thông tin hiển thị sang DTO response cho API.
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

    // Hàm toResponseSnapshot: Nhận các trường lịch cũ/mới; trả về object snapshot lồng trong response hoặc null khi
    // không có dữ liệu thay đổi.
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

    // Hàm normalize: Nhận lý do thay đổi lịch; chuyển lý do null/rỗng thành null và trim nội dung trước khi ghi vào
    // lịch sử.
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // Hàm Snapshot: Là value object chứa toàn bộ trạng thái lịch (ngày, giờ, phòng, địa điểm, hội đồng, trạng thái) tại
    // trước hoặc sau một thao tác.
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

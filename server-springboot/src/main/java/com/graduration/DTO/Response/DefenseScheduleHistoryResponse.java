package com.graduration.DTO.Response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.graduration.Constain.DefenseScheduleHistoryActionConstain;
import com.graduration.Constain.DefenseScheduleStatusConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DefenseScheduleHistoryResponse {
    Long historyId;
    Long scheduleId;
    DefenseScheduleHistoryActionConstain action;
    DefenseScheduleStatusConstain previousStatus;
    DefenseScheduleStatusConstain newStatus;
    ScheduleSnapshot oldSchedule;
    ScheduleSnapshot newSchedule;
    String reason;
    String changedByUserId;
    String changedByUsername;
    LocalDateTime changedAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleSnapshot {
        LocalDate defenseDate;
        LocalTime startTime;
        LocalTime endTime;
        String room;
        String location;
        Long committeeId;
        String committeeName;
    }
}

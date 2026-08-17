package com.graduration.DTO.Request;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.graduration.Constain.DefenseSessionConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RescheduleDefenseRequest {
    @NotNull(message = "DEFENSE_COMMITTEE_NOT_FOUND")
    Long committeeId;

    @NotNull(message = "DEFENSE_SCHEDULE_DATE_NOT_BLANK")
    LocalDate defenseDate;

    @NotNull(message = "DEFENSE_SCHEDULE_START_TIME_NOT_BLANK")
    LocalTime startTime;

    @NotNull(message = "DEFENSE_SCHEDULE_END_TIME_NOT_BLANK")
    LocalTime endTime;

    @NotBlank(message = "DEFENSE_SCHEDULE_ROOM_NOT_BLANK")
    String room;

    @NotBlank(message = "DEFENSE_SCHEDULE_LOCATION_NOT_BLANK")
    String location;

    DefenseSessionConstain session;

    @NotBlank(message = "DEFENSE_SCHEDULE_REASON_NOT_BLANK")
    String reason;
}

package com.graduration.DTO.Request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public class BulkDefenseScheduleRequest {
    @NotNull(message = "DEFENSE_COMMITTEE_NOT_FOUND")
    Long committeeId;

    @NotNull(message = "DEFENSE_SCHEDULE_DATE_NOT_BLANK")
    LocalDate defenseDate;

    @NotBlank(message = "DEFENSE_SCHEDULE_ROOM_NOT_BLANK")
    String room;

    @NotBlank(message = "DEFENSE_SCHEDULE_LOCATION_NOT_BLANK")
    String location;

    DefenseSessionConstain session;
    String note;

    @Valid
    @NotEmpty(message = "TOPIC_NOT_FOUND")
    List<DefenseScheduleTopicTimeRequest> topicSchedules;
}

package com.graduration.DTO.Request;

import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DefenseScheduleTopicTimeRequest {
    @NotNull(message = "TOPIC_NOT_FOUND")
    Long topicId;

    @NotNull(message = "DEFENSE_SCHEDULE_START_TIME_NOT_BLANK")
    LocalTime startTime;

    @NotNull(message = "DEFENSE_SCHEDULE_END_TIME_NOT_BLANK")
    LocalTime endTime;
}

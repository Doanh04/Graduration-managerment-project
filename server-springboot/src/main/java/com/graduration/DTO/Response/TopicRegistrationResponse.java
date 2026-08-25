package com.graduration.DTO.Response;

import java.time.LocalDateTime;

import com.graduration.Constain.TopicRegistrationStatusConstain;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicRegistrationResponse {
    Long registrationId;
    Long topicId;
    String topicTitle;
    String technology;
    Long teamId;
    String teamName;
    String studentCode;
    String studentName;
    Long defensePeriodId;
    String defensePeriodName;
    Integer priority;
    TopicRegistrationStatusConstain status;
    String preferredSupervisorId;
    String preferredSupervisorName;
    String note;
    String rejectionReason;
    LocalDateTime submittedAt;
    LocalDateTime reviewedAt;
}

package com.graduration.DTO.Response;

import java.time.LocalDateTime;

import com.graduration.Constain.SupervisorAssignmentStatusConstain;
import com.graduration.Constain.SupervisorRoleConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicSupervisorResponse {
    Long assignmentId;
    Long topicId;
    String topicTitle;
    Long defensePeriodId;
    String lectureId;
    String lectureCode;
    String lectureName;
    SupervisorRoleConstain role;
    SupervisorAssignmentStatusConstain status;
    LocalDateTime assignedAt;
    LocalDateTime endedAt;
    String assignedByUserId;
    String assignedByUsername;
    String note;
}

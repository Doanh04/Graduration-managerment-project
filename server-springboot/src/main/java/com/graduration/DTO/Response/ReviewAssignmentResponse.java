package com.graduration.DTO.Response;

import java.time.LocalDateTime;

import com.graduration.Constain.ReviewAssignmentStatusConstain;
import com.graduration.Constain.ReviewRecommendationConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewAssignmentResponse {
    Long assignmentId;
    Long topicId;
    String topicTitle;
    Long teamId;
    String teamName;
    Long defensePeriodId;
    String lectureId;
    String lectureCode;
    String lectureName;
    ReviewAssignmentStatusConstain status;
    LocalDateTime assignedAt;
    LocalDateTime deadline;
    LocalDateTime submittedAt;
    LocalDateTime reviewedAt;
    LocalDateTime cancelledAt;
    boolean overdue;
    ReviewRecommendationConstain recommendation;
    String reviewComment;
    String assignedByUserId;
    String assignedByUsername;
    String reviewedByUserId;
    String note;
    String cancelledReason;
}

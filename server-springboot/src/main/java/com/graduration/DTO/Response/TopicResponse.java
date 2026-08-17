package com.graduration.DTO.Response;

import java.time.LocalDateTime;

import com.graduration.Constain.CategoryTopicConstain;
import com.graduration.Constain.TopicStatusConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicResponse {
    Long topicId;
    String title;
    String description;
    String objective;
    String technology;
    CategoryTopicConstain categoryTopic;
    TopicStatusConstain status;
    String createdBy;
    String rejectionReason;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Long defensePeriodId;
    String defensePeriodName;
    Integer academicYearId;
    String academicYear;
    Long teamId;
    String teamName;
}

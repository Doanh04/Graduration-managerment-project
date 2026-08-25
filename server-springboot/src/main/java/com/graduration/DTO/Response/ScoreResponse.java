package com.graduration.DTO.Response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.graduration.Constain.ScoreStatusConstain;
import com.graduration.Constain.ScoreTypeConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScoreResponse {
    Long scoreId;
    BigDecimal totalScore;
    ScoreTypeConstain scoreType;
    String comment;
    ScoreStatusConstain status;
    String studentId;
    String studentCode;
    String studentName;
    Long teamId;
    String teamName;
    Long topicId;
    String topicTitle;
    Long defensePeriodId;
    String lecturerId;
    String lecturerCode;
    String lecturerName;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime submittedAt;
    LocalDateTime publishedAt;
}

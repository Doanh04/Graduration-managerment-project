package com.graduration.DTO.Response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.graduration.Constain.ScoreStatusConstain;

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
    String createdByUserId;
    String createdByUsername;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime submittedAt;
    LocalDateTime publishedAt;
    List<Detail> details;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Detail {
        Long criterionId;
        String criterionCode;
        String criterionName;
        BigDecimal maxScore;
        BigDecimal weight;
        BigDecimal score;
        BigDecimal weightedScore;
        String comment;
    }
}

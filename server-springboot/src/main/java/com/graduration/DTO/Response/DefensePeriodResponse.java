package com.graduration.DTO.Response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.graduration.Constain.DefensePeriodConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DefensePeriodResponse {
    Long defensePeriodId;
    String periodName;
    LocalDate startDate;
    LocalDate endDate;
    LocalDateTime finalSubmissionDeadline;
    String projectType;
    DefensePeriodConstain status;
    Integer academicId;
    String academicYear;
    int topicCount;
    int milestoneCount;
}

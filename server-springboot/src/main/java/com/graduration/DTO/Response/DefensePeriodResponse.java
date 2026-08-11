package com.graduration.DTO.Response;

import java.time.LocalDate;

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
    String projectType;
    DefensePeriodConstain status;
    Integer academicId;
    String academicYear;
    int topicCount;
    int milestoneCount;
}

package com.graduration.DTO.Response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.graduration.Constain.DefensePeriodConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AcademicYearResponse {
    Integer academicId;
    String academicYear;
    String description;

    @Builder.Default
    List<DefensePeriodSummary> defensePeriods = new ArrayList<>();

    @Builder.Default
    List<DefenseCommitteeSummary> defenseCommittees = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DefensePeriodSummary {
        Long defensePeriodId;
        String periodName;
        LocalDate startDate;
        LocalDate endDate;
        String projectType;
        DefensePeriodConstain status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DefenseCommitteeSummary {
        Long committeeId;
        String committeeName;
    }
}

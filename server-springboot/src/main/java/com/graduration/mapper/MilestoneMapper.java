package com.graduration.mapper;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.graduration.DTO.Response.MilestoneResponse;
import com.graduration.entity.AcademicYearEntity;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.MilesStoneEntity;

@Component
public class MilestoneMapper {
    public MilestoneResponse toResponse(MilesStoneEntity milestone) {
        DefensePeriodEntity period = milestone.getDefensePeriod();
        AcademicYearEntity academicYear = period == null ? null : period.getAcademicYear();
        return MilestoneResponse.builder()
                .milestoneId(milestone.getIdMilesStone())
                .milestoneName(milestone.getMilesStoneName())
                .description(milestone.getDescription())
                .startAt(milestone.getStartAt())
                .deadline(milestone.getDeadLine())
                .milestoneType(milestone.getMilestoneType())
                .status(milestone.getStatus())
                .allowLateSubmission(milestone.getAllowLateSubmission())
                .required(milestone.getRequired())
                .maxFileSize(milestone.getMaxFileSize())
                .allowedFileTypes(parseAllowedFileTypes(milestone.getAllowedFileTypes()))
                .defensePeriodId(period == null ? null : period.getID_Defense())
                .defensePeriodName(period == null ? null : period.getPeriodName())
                .academicYearId(academicYear == null ? null : academicYear.getAcademicId())
                .academicYear(academicYear == null ? null : academicYear.getAcademicYear())
                .submissionCount(
                        milestone.getSubmistion() == null
                                ? 0
                                : milestone.getSubmistion().size())
                .build();
    }

    private List<String> parseAllowedFileTypes(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }
}

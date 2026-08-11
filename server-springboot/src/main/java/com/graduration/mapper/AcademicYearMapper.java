package com.graduration.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.graduration.DTO.Request.AcademicYearRequest;
import com.graduration.DTO.Response.AcademicYearResponse;
import com.graduration.entity.AcademicYearEntity;
import com.graduration.entity.DefenseCommitteesEntity;
import com.graduration.entity.DefensePeriodEntity;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface AcademicYearMapper {
    @Mapping(target = "academicId", ignore = true)
    @Mapping(target = "defensePeriod", ignore = true)
    @Mapping(target = "defenseCommittees", ignore = true)
    AcademicYearEntity toAcademicYearEntity(AcademicYearRequest request);

    @Mapping(target = "academicId", ignore = true)
    @Mapping(target = "defensePeriod", ignore = true)
    @Mapping(target = "defenseCommittees", ignore = true)
    void updateAcademicYear(AcademicYearRequest request, @MappingTarget AcademicYearEntity academicYear);

    @Mapping(source = "defensePeriod", target = "defensePeriods")
    AcademicYearResponse toAcademicYearResponse(AcademicYearEntity academicYear);

    @Mapping(source = "ID_Defense", target = "defensePeriodId")
    AcademicYearResponse.DefensePeriodSummary toDefensePeriodSummary(DefensePeriodEntity defensePeriod);

    @Mapping(source = "idComittees", target = "committeeId")
    @Mapping(source = "comitteesName", target = "committeeName")
    AcademicYearResponse.DefenseCommitteeSummary toDefenseCommitteeSummary(DefenseCommitteesEntity committee);
}

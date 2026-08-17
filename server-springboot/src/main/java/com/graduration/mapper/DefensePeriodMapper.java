package com.graduration.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.graduration.DTO.Request.DefensePeriodRequest;
import com.graduration.DTO.Response.DefensePeriodResponse;
import com.graduration.entity.DefensePeriodEntity;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface DefensePeriodMapper {
    @Mapping(target = "ID_Defense", ignore = true)
    @Mapping(target = "academicYear", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "milesStone", ignore = true)
    @Mapping(target = "graduationEnrollments", ignore = true)
    @Mapping(target = "defenseCommittees", ignore = true)
    @Mapping(target = "scoreCriteria", ignore = true)
    @Mapping(target = "templates", ignore = true)
    DefensePeriodEntity toDefensePeriodEntity(DefensePeriodRequest request);

    @Mapping(target = "ID_Defense", ignore = true)
    @Mapping(target = "academicYear", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "milesStone", ignore = true)
    @Mapping(target = "graduationEnrollments", ignore = true)
    @Mapping(target = "defenseCommittees", ignore = true)
    @Mapping(target = "scoreCriteria", ignore = true)
    @Mapping(target = "templates", ignore = true)
    void updateDefensePeriod(DefensePeriodRequest request, @MappingTarget DefensePeriodEntity defensePeriod);

    @Mapping(source = "ID_Defense", target = "defensePeriodId")
    @Mapping(source = "academicYear.academicId", target = "academicId")
    @Mapping(source = "academicYear.academicYear", target = "academicYear")
    @Mapping(
            target = "topicCount",
            expression = "java(defensePeriod.getTopic() == null ? 0 : defensePeriod.getTopic().size())")
    @Mapping(
            target = "milestoneCount",
            expression = "java(defensePeriod.getMilesStone() == null ? 0 : defensePeriod.getMilesStone().size())")
    DefensePeriodResponse toDefensePeriodResponse(DefensePeriodEntity defensePeriod);
}

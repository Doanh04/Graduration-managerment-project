package com.graduration.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.graduration.Constain.SupervisorAssignmentStatusConstain;
import com.graduration.Constain.SupervisorRoleConstain;
import com.graduration.DTO.Request.CreateTopicRequest;
import com.graduration.DTO.Request.UpdateTopicRequest;
import com.graduration.DTO.Response.TopicResponse;
import com.graduration.entity.TopicEntity;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TopicMapper {
    @Mapping(target = "idTopic", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "defensePeriod", ignore = true)
    @Mapping(target = "topicSuperVisorEntities", ignore = true)
    @Mapping(target = "reviewAssignment", ignore = true)
    @Mapping(target = "defenseSchedule", ignore = true)
    @Mapping(target = "topicRegistrations", ignore = true)
    @Mapping(target = "scores", ignore = true)
    TopicEntity toEntity(CreateTopicRequest request);

    @Mapping(target = "idTopic", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "defensePeriod", ignore = true)
    @Mapping(target = "topicSuperVisorEntities", ignore = true)
    @Mapping(target = "reviewAssignment", ignore = true)
    @Mapping(target = "defenseSchedule", ignore = true)
    @Mapping(target = "topicRegistrations", ignore = true)
    @Mapping(target = "scores", ignore = true)
    void update(UpdateTopicRequest request, @MappingTarget TopicEntity topic);

    @Mapping(source = "idTopic", target = "topicId")
    @Mapping(source = "defensePeriod.ID_Defense", target = "defensePeriodId")
    @Mapping(source = "defensePeriod.periodName", target = "defensePeriodName")
    @Mapping(source = "defensePeriod.academicYear.academicId", target = "academicYearId")
    @Mapping(source = "defensePeriod.academicYear.academicYear", target = "academicYear")
    @Mapping(source = "team.idTeam", target = "teamId")
    @Mapping(source = "team.nameTeam", target = "teamName")
    @Mapping(target = "proposedStudents", ignore = true)
    @Mapping(target = "hasActiveSupervisor", expression = "java(hasActivePrimarySupervisor(topic))")
    TopicResponse toResponse(TopicEntity topic);

    default boolean hasActivePrimarySupervisor(TopicEntity topic) {
        return topic != null
                && topic.getTopicSuperVisorEntities() != null
                && topic.getTopicSuperVisorEntities().stream()
                        .anyMatch(assignment -> assignment.getStatus() == SupervisorAssignmentStatusConstain.ACTIVE
                                && assignment.getSupervisorRole() == SupervisorRoleConstain.PRIMARY);
    }
}

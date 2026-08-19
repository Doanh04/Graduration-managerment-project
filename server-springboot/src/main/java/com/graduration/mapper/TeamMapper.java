package com.graduration.mapper;

import java.util.Collections;
import java.util.List;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.graduration.DTO.Request.TeamRequest;
import com.graduration.DTO.Response.TeamResponse;
import com.graduration.entity.StudentEntity;
import com.graduration.entity.TeamEntity;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TeamMapper {
    @Mapping(target = "idTeam", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "studentEntities", ignore = true)
    @Mapping(target = "submistion", ignore = true)
    @Mapping(target = "score", ignore = true)
    @Mapping(target = "topicRegistrations", ignore = true)
    TeamEntity toTeamEntity(TeamRequest request);

    @Mapping(target = "idTeam", ignore = true)
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "studentEntities", ignore = true)
    @Mapping(target = "submistion", ignore = true)
    @Mapping(target = "score", ignore = true)
    @Mapping(target = "topicRegistrations", ignore = true)
    void updateTeam(TeamRequest request, @MappingTarget TeamEntity team);

    @Mapping(source = "topic.idTopic", target = "topicId")
    @Mapping(source = "topic.title", target = "topicTitle")
    @Mapping(source = "topic.description", target = "topicDescription")
    @Mapping(source = "studentEntities", target = "students")
    TeamResponse toTeamResponse(TeamEntity team);

    default List<TeamResponse.StudentSummary> toStudentSummaries(List<StudentEntity> students) {
        if (students == null) {
            return Collections.emptyList();
        }
        return students.stream().map(this::toStudentSummary).toList();
    }

    @Mapping(source = "fullNameStudent", target = "fullName")
    @Mapping(source = "classEntity.classId", target = "classId")
    @Mapping(source = "classEntity.classCode", target = "classCode")
    TeamResponse.StudentSummary toStudentSummary(StudentEntity student);
}

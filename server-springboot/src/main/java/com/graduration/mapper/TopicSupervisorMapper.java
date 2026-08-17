package com.graduration.mapper;

import org.springframework.stereotype.Component;

import com.graduration.DTO.Response.TopicSupervisorResponse;
import com.graduration.entity.TopicSuperVisorEntity;

@Component
public class TopicSupervisorMapper {
    public TopicSupervisorResponse toResponse(TopicSuperVisorEntity entity) {
        return TopicSupervisorResponse.builder()
                .assignmentId(entity.getIdSuperVisor())
                .topicId(entity.getTopic().getIdTopic())
                .topicTitle(entity.getTopic().getTitle())
                .defensePeriodId(entity.getTopic().getDefensePeriod().getID_Defense())
                .lectureId(entity.getLecture().getLectureId())
                .lectureCode(entity.getLecture().getLectureCode())
                .lectureName(entity.getLecture().getFullNameLecture())
                .role(entity.getSupervisorRole())
                .status(entity.getStatus())
                .assignedAt(entity.getAssignedAt())
                .endedAt(entity.getEndedAt())
                .assignedByUserId(
                        entity.getAssignedBy() == null
                                ? null
                                : entity.getAssignedBy().getUserId())
                .assignedByUsername(
                        entity.getAssignedBy() == null
                                ? null
                                : entity.getAssignedBy().getUserName())
                .note(entity.getNote())
                .build();
    }
}

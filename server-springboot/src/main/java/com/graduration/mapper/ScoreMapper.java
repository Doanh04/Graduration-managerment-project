package com.graduration.mapper;

import org.springframework.stereotype.Component;

import com.graduration.DTO.Response.ScoreResponse;
import com.graduration.entity.ScoreEntity;

@Component
public class ScoreMapper {
    public ScoreResponse toScoreResponse(ScoreEntity score) {
        return ScoreResponse.builder()
                .scoreId(score.getId())
                .totalScore(score.getScore())
                .scoreType(score.getScoreType())
                .comment(score.getComment())
                .status(score.getStatus())
                .studentId(score.getStudent().getIdStudent())
                .studentCode(score.getStudent().getStudentCode())
                .studentName(score.getStudent().getFullNameStudent())
                .teamId(
                        score.getTopic().getTeam() == null
                                ? null
                                : score.getTopic().getTeam().getIdTeam())
                .teamName(
                        score.getTopic().getTeam() == null
                                ? null
                                : score.getTopic().getTeam().getNameTeam())
                .topicId(score.getTopic().getIdTopic())
                .topicTitle(score.getTopic().getTitle())
                .defensePeriodId(score.getTopic().getDefensePeriod().getID_Defense())
                .lecturerId(
                        score.getLecture() == null ? null : score.getLecture().getLectureId())
                .lecturerCode(
                        score.getLecture() == null ? null : score.getLecture().getLectureCode())
                .lecturerName(
                        score.getLecture() == null ? null : score.getLecture().getFullNameLecture())
                .createdAt(score.getCreatedAt())
                .updatedAt(score.getUpdatedAt())
                .submittedAt(score.getSubmittedAt())
                .publishedAt(score.getPublishedAt())
                .build();
    }
}

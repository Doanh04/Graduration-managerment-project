package com.graduration.mapper;

import org.springframework.stereotype.Component;

import com.graduration.DTO.Response.ScoreCriterionResponse;
import com.graduration.DTO.Response.ScoreResponse;
import com.graduration.entity.ScoreCriterionEntity;
import com.graduration.entity.ScoreDetailEntity;
import com.graduration.entity.ScoreEntity;

@Component
public class ScoreMapper {
    public ScoreCriterionResponse toCriterionResponse(ScoreCriterionEntity criterion) {
        return ScoreCriterionResponse.builder()
                .criterionId(criterion.getCriterionId())
                .criterionCode(criterion.getCriterionCode())
                .criterionName(criterion.getCriterionName())
                .description(criterion.getDescription())
                .maxScore(criterion.getMaxScore())
                .weight(criterion.getWeight())
                .displayOrder(criterion.getDisplayOrder())
                .active(criterion.getActive())
                .defensePeriodId(criterion.getDefensePeriod().getID_Defense())
                .defensePeriodName(criterion.getDefensePeriod().getPeriodName())
                .build();
    }

    public ScoreResponse toScoreResponse(ScoreEntity score) {
        return ScoreResponse.builder()
                .scoreId(score.getId())
                .totalScore(score.getScore())
                .comment(score.getComment())
                .status(score.getStatus())
                .studentId(score.getStudent().getIdStudent())
                .studentCode(score.getStudent().getStudentCode())
                .studentName(score.getStudent().getFullNameStudent())
                .teamId(score.getTeam().getIdTeam())
                .teamName(score.getTeam().getNameTeam())
                .topicId(score.getTopic().getIdTopic())
                .topicTitle(score.getTopic().getTitle())
                .defensePeriodId(score.getTopic().getDefensePeriod().getID_Defense())
                .createdByUserId(
                        score.getCreatedBy() == null
                                ? null
                                : score.getCreatedBy().getUserId())
                .createdByUsername(
                        score.getCreatedBy() == null
                                ? null
                                : score.getCreatedBy().getUserName())
                .createdAt(score.getCreatedAt())
                .updatedAt(score.getUpdatedAt())
                .submittedAt(score.getSubmittedAt())
                .publishedAt(score.getPublishedAt())
                .details(score.getDetails().stream().map(this::toDetail).toList())
                .build();
    }

    private ScoreResponse.Detail toDetail(ScoreDetailEntity detail) {
        ScoreCriterionEntity criterion = detail.getCriterion();
        return ScoreResponse.Detail.builder()
                .criterionId(criterion.getCriterionId())
                .criterionCode(criterion.getCriterionCode())
                .criterionName(criterion.getCriterionName())
                .maxScore(criterion.getMaxScore())
                .weight(criterion.getWeight())
                .score(detail.getScoreValue())
                .weightedScore(detail.getWeightedScore())
                .comment(detail.getComment())
                .build();
    }
}

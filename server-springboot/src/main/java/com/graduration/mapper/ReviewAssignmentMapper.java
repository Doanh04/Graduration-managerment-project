package com.graduration.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.graduration.Constain.ReviewAssignmentStatusConstain;
import com.graduration.DTO.Response.ReviewAssignmentResponse;
import com.graduration.entity.ReviewAssignmentEntity;

@Component
public class ReviewAssignmentMapper {
    public ReviewAssignmentResponse toResponse(ReviewAssignmentEntity entity) {
        boolean terminal = entity.getStatus() == ReviewAssignmentStatusConstain.APPROVED
                || entity.getStatus() == ReviewAssignmentStatusConstain.CANCELLED;
        boolean overdue =
                !terminal && entity.getDeadline() != null && LocalDateTime.now().isAfter(entity.getDeadline());
        return ReviewAssignmentResponse.builder()
                .assignmentId(entity.getReviewAssignmentId())
                .topicId(entity.getTopic().getIdTopic())
                .topicTitle(entity.getTopic().getTitle())
                .teamId(
                        entity.getTopic().getTeam() == null
                                ? null
                                : entity.getTopic().getTeam().getIdTeam())
                .teamName(
                        entity.getTopic().getTeam() == null
                                ? null
                                : entity.getTopic().getTeam().getNameTeam())
                .defensePeriodId(entity.getTopic().getDefensePeriod().getID_Defense())
                .lectureId(entity.getLecture().getLectureId())
                .lectureCode(entity.getLecture().getLectureCode())
                .lectureName(entity.getLecture().getFullNameLecture())
                .status(entity.getStatus())
                .assignedAt(entity.getAssignedAt())
                .deadline(entity.getDeadline())
                .submittedAt(entity.getSubmittedAt())
                .reviewedAt(entity.getReviewedAt())
                .cancelledAt(entity.getCancelledAt())
                .overdue(overdue)
                .recommendation(entity.getRecommendation())
                .reviewComment(entity.getReviewComment())
                .assignedByUserId(
                        entity.getAssignedBy() == null
                                ? null
                                : entity.getAssignedBy().getUserId())
                .assignedByUsername(
                        entity.getAssignedBy() == null
                                ? null
                                : entity.getAssignedBy().getUserName())
                .reviewedByUserId(
                        entity.getReviewedBy() == null
                                ? null
                                : entity.getReviewedBy().getUserId())
                .note(entity.getNote())
                .cancelledReason(entity.getCancelledReason())
                .build();
    }
}

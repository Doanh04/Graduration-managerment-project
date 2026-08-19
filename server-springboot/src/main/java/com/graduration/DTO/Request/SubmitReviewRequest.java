package com.graduration.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.graduration.Constain.ReviewRecommendationConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmitReviewRequest {
    @NotBlank(message = "REVIEW_COMMENT_NOT_BLANK")
    String reviewComment;

    @NotNull(message = "REVIEW_RECOMMENDATION_NOT_BLANK")
    ReviewRecommendationConstain recommendation;
}

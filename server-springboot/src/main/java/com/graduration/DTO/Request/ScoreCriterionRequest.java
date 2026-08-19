package com.graduration.DTO.Request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScoreCriterionRequest {
    @NotBlank(message = "SCORE_CRITERION_CODE_NOT_BLANK")
    String criterionCode;

    @NotBlank(message = "SCORE_CRITERION_NAME_NOT_BLANK")
    String criterionName;

    String description;

    @NotNull(message = "SCORE_CRITERION_MAX_SCORE_INVALID")
    @DecimalMin(value = "0.01", message = "SCORE_CRITERION_MAX_SCORE_INVALID")
    @DecimalMax(value = "10.00", message = "SCORE_CRITERION_MAX_SCORE_INVALID")
    BigDecimal maxScore;

    @NotNull(message = "SCORE_CRITERION_WEIGHT_INVALID")
    @DecimalMin(value = "0.01", message = "SCORE_CRITERION_WEIGHT_INVALID")
    @DecimalMax(value = "100.00", message = "SCORE_CRITERION_WEIGHT_INVALID")
    BigDecimal weight;

    Integer displayOrder;
    Boolean active;
}

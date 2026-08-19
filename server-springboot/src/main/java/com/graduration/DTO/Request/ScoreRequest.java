package com.graduration.DTO.Request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScoreRequest {
    String comment;

    @NotEmpty(message = "SCORE_DETAILS_NOT_EMPTY")
    List<@Valid Detail> details;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Detail {
        @NotNull(message = "SCORE_CRITERION_NOT_FOUND")
        Long criterionId;

        @NotNull(message = "SCORE_VALUE_INVALID")
        @DecimalMin(value = "0.00", message = "SCORE_VALUE_INVALID")
        BigDecimal score;

        String comment;
    }
}

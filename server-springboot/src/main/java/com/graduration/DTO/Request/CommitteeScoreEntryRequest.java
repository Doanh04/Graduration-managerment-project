package com.graduration.DTO.Request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitteeScoreEntryRequest {
    @NotBlank(message = "LECTURER_NOT_FOUND")
    private String lecturerId;

    @NotNull(message = "SCORE_VALUE_INVALID")
    @DecimalMin(value = "0.00", message = "SCORE_VALUE_INVALID")
    @DecimalMax(value = "10.00", message = "SCORE_VALUE_INVALID")
    private BigDecimal score;

    @NotBlank(message = "SCORE_COMMENT_NOT_BLANK")
    private String comment;
}

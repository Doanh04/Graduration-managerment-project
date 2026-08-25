package com.graduration.DTO.Request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
    @NotNull(message = "SCORE_VALUE_INVALID")
    @DecimalMin(value = "0.00", message = "SCORE_VALUE_INVALID")
    @DecimalMax(value = "10.00", message = "SCORE_VALUE_INVALID")
    BigDecimal score;

    String comment;
}

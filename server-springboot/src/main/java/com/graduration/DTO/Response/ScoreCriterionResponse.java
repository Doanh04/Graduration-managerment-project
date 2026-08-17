package com.graduration.DTO.Response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScoreCriterionResponse {
    Long criterionId;
    String criterionCode;
    String criterionName;
    String description;
    BigDecimal maxScore;
    BigDecimal weight;
    Integer displayOrder;
    Boolean active;
    Long defensePeriodId;
    String defensePeriodName;
}

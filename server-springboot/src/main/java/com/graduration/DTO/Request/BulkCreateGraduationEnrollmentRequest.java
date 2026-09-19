package com.graduration.DTO.Request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreateGraduationEnrollmentRequest {
    @NotEmpty
    private List<String> studentCodes;

    @NotNull(message = "DEFENSE_PERIOD_NOT_FOUND")
    private Long defensePeriodId;

    private String note;
}

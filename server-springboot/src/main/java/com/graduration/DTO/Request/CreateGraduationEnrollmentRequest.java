package com.graduration.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGraduationEnrollmentRequest {
    @NotBlank(message = "USER_NOT_FOUND")
    private String studentCode;

    @NotNull(message = "DEFENSE_PERIOD_NOT_FOUND")
    private Long defensePeriodId;

    private String note;
}

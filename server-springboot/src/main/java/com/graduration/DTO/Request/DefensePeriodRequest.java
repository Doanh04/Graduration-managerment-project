package com.graduration.DTO.Request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.graduration.Constain.DefensePeriodConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DefensePeriodRequest {
    @NotBlank(message = "DEFENSE_PERIOD_NAME_NOT_BLANK")
    @Size(max = 255, message = "INVALID_KEY")
    String periodName;

    @NotNull(message = "START_DATE_NOT_BLANK")
    LocalDate startDate;

    @NotNull(message = "END_DATE_NOT_BLANK")
    LocalDate endDate;

    @Size(max = 255, message = "INVALID_KEY")
    String projectType;

    @NotNull(message = "STATUS_NOT_FOUND")
    DefensePeriodConstain status;

    @NotNull(message = "ACADEMIC_YEAR_NOT_FOUND")
    Integer academicId;
}

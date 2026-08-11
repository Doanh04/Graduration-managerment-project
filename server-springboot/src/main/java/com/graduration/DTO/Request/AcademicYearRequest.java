package com.graduration.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AcademicYearRequest {
    @NotBlank(message = "ACADEMIC_YEAR_NOT_BLANK")
    @Pattern(regexp = "^\\d{4}\\s*[-/]\\s*\\d{4}$", message = "ACADEMIC_YEAR_INVALID")
    String academicYear;

    @Size(max = 1000, message = "INVALID_KEY")
    String description;
}

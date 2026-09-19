package com.graduration.DTO.Request;

import jakarta.validation.constraints.NotNull;

import com.graduration.Constain.EnrollmentStatusConstain;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGraduationEnrollmentRequest {
    @NotNull
    private EnrollmentStatusConstain status;

    private String note;
}

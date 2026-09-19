package com.graduration.DTO.Response;

import java.time.LocalDateTime;

import com.graduration.Constain.EnrollmentStatusConstain;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraduationEnrollmentResponse {
    private Long enrollmentId;
    private String studentId;
    private String studentCode;
    private String studentName;
    private String classCode;
    private Long teamId;
    private String teamName;
    private Long defensePeriodId;
    private String defensePeriodName;
    private String academicYear;
    private EnrollmentStatusConstain status;
    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
    private String note;
}

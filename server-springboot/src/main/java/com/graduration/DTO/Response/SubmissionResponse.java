package com.graduration.DTO.Response;

import java.time.LocalDateTime;

import com.graduration.Constain.SubmissionStatusConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmissionResponse {
    Long submissionId;
    Long milestoneId;
    String milestoneName;
    Long teamId;
    String teamName;
    String studentId;
    String studentCode;
    String submittedByName;
    String fileName;
    String contentType;
    Long fileSize;
    String checksum;
    Boolean late;
    String note;
    LocalDateTime submittedAt;
    LocalDateTime updatedAt;
    Integer version;
    SubmissionStatusConstain status;
}

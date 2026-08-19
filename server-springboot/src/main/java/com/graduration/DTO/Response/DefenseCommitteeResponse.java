package com.graduration.DTO.Response;

import java.time.LocalDateTime;

import com.graduration.Constain.DefenseCommitteeStatusConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DefenseCommitteeResponse {
    Long committeeId;
    String committeeName;
    String description;
    DefenseCommitteeStatusConstain status;
    Long defensePeriodId;
    String defensePeriodName;
    Integer academicYearId;
    String academicYear;
    long activeMemberCount;
    int scheduleCount;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime activatedAt;
    String deactivationReason;
    String createdByUserId;
    String createdByUsername;
}

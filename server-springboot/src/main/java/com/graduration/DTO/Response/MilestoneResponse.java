package com.graduration.DTO.Response;

import java.time.LocalDateTime;
import java.util.List;

import com.graduration.Constain.MilesStoneStatusConstain;
import com.graduration.Constain.MilesStoneTypeConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MilestoneResponse {
    Long milestoneId;
    String milestoneName;
    String description;
    LocalDateTime startAt;
    LocalDateTime deadline;
    MilesStoneTypeConstain milestoneType;
    MilesStoneStatusConstain status;
    Boolean allowLateSubmission;
    Boolean required;
    Long maxFileSize;
    List<String> allowedFileTypes;
    Long defensePeriodId;
    String defensePeriodName;
    Integer academicYearId;
    String academicYear;
    int submissionCount;
}

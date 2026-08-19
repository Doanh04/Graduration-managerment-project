package com.graduration.DTO.Request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.graduration.Constain.MilesStoneTypeConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateMilestoneRequest {
    @NotBlank(message = "MILESTONE_NAME_NOT_BLANK")
    String milestoneName;

    String description;

    @NotNull(message = "MILESTONE_START_AT_NOT_BLANK")
    LocalDateTime startAt;

    @NotNull(message = "MILESTONE_DEADLINE_NOT_BLANK")
    LocalDateTime deadline;

    @NotNull(message = "MILESTONE_TYPE_NOT_BLANK")
    MilesStoneTypeConstain milestoneType;

    Boolean allowLateSubmission;
    Boolean required;

    @Positive(message = "MILESTONE_MAX_FILE_SIZE_INVALID")
    Long maxFileSize;

    String allowedFileTypes;
}

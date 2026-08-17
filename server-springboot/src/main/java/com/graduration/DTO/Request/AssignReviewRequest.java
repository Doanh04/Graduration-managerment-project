package com.graduration.DTO.Request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssignReviewRequest {
    @NotBlank(message = "LECTURER_NOT_BLANK")
    String lectureId;

    @NotNull(message = "REVIEW_DEADLINE_NOT_BLANK")
    LocalDateTime deadline;

    String note;
}

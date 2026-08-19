package com.graduration.DTO.Request;

import jakarta.validation.constraints.NotBlank;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeactivateTopicSupervisorRequest {
    @NotBlank(message = "SUPERVISOR_DEACTIVATION_REASON_NOT_BLANK")
    String reason;
}

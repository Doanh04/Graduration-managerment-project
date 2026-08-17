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
public class RejectTopicRequest {
    @NotBlank(message = "TOPIC_REJECTION_REASON_NOT_BLANK")
    String reason;
}

package com.graduration.DTO.Request;

import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateTopicRegistrationRequest {
    @NotNull(message = "TOPIC_NOT_FOUND")
    Long topicId;

    Integer priority;
    String preferredSupervisorId;
    String note;
}

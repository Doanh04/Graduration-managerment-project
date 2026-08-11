package com.graduration.DTO.Request;

import jakarta.validation.constraints.NotBlank;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LibraryTopicRequest {
    @NotBlank(message = "LIBRARY_TOPIC_TITLE_NOT_BLANK")
    String title;

    String description;

    String objective;

    String technology;
}

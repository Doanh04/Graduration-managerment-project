package com.graduration.DTO.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LibraryTopicResponse {
    Long idLibraryTopic;
    String title;
    String description;
    String objective;
    String technology;
}

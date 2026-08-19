package com.graduration.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.graduration.Constain.CategoryTopicConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateTopicRequest {
    @NotBlank(message = "TOPIC_TITLE_NOT_BLANK")
    String title;

    String description;
    String objective;
    String technology;

    @NotNull(message = "TOPIC_CATEGORY_NOT_BLANK")
    CategoryTopicConstain categoryTopic;

    @NotNull(message = "DEFENSE_PERIOD_NOT_FOUND")
    Long defensePeriodId;
}

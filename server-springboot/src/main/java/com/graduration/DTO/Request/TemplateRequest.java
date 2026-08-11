package com.graduration.DTO.Request;

import jakarta.validation.constraints.NotBlank;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TemplateRequest {
    @NotBlank(message = "TEMPLATE_NAME_NOT_BLANK")
    String templateName;

    String description;
    String filePath;
}

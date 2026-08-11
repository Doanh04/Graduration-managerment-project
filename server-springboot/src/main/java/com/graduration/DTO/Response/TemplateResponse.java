package com.graduration.DTO.Response;

import java.time.LocalDate;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TemplateResponse {
    Integer templateId;
    String templateName;
    String description;
    String filePath;
    LocalDate createAt;
}

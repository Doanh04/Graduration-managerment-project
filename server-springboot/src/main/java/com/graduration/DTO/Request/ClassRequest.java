package com.graduration.DTO.Request;

import jakarta.validation.constraints.NotBlank;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClassRequest {
    @NotBlank(message = "CLASS_CODE_NOT_BLANK")
    String classCode;

    @NotBlank(message = "CLASS_NAME_NOT_BLANK")
    String nameClass;

    @NotBlank(message = "MAJOR_ID_NOT_BLANK")
    String majorId;

    String description;
}

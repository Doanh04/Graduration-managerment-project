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
public class DefenseCommitteeRequest {
    @NotBlank(message = "DEFENSE_COMMITTEE_NAME_NOT_BLANK")
    String committeeName;

    String description;
}

package com.graduration.DTO.Request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeamRequest {
    @NotBlank(message = "TEAM_NAME_NOT_BLANK")
    @Size(max = 255, message = "INVALID_KEY")
    String nameTeam;

    @Size(max = 1000, message = "INVALID_KEY")
    String description;

    LocalDate joinDate;

    @Size(max = 100, message = "INVALID_KEY")
    String role;

    Long topicId;
}

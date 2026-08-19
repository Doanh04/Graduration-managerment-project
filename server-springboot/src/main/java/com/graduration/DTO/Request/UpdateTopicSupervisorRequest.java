package com.graduration.DTO.Request;

import jakarta.validation.constraints.NotNull;

import com.graduration.Constain.SupervisorRoleConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateTopicSupervisorRequest {
    @NotNull(message = "SUPERVISOR_ROLE_NOT_BLANK")
    SupervisorRoleConstain role;

    String note;
}

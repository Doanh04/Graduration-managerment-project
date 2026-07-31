package com.graduration.DTO.Request;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import com.graduration.Constain.PermissionConstain;
import com.graduration.Constain.RoleNameConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateRoleRequest {
    @NotNull(message = "ROLE_NOT_BLANK")
    RoleNameConstain roleName;

    String description;

    @Builder.Default
    Set<PermissionConstain> permissions = new HashSet<>();
}

package com.graduration.DTO.Response;

import java.util.HashSet;
import java.util.Set;

import com.graduration.Constain.PermissionConstain;
import com.graduration.Constain.RoleConstain;
import com.graduration.Constain.RoleNameConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleResponse {
    RoleConstain role;
    RoleNameConstain roleName;
    String description;

    @Builder.Default
    Set<PermissionConstain> permissions = new HashSet<>();
}

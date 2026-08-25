package com.graduration.DTO.Request;

import java.util.HashSet;
import java.util.Set;

import com.graduration.Constain.RoleConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateUserRolesRequest {
    @Builder.Default
    Set<RoleConstain> roles = new HashSet<>();
}

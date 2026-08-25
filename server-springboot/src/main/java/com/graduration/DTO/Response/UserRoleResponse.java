package com.graduration.DTO.Response;

import java.util.HashSet;
import java.util.Set;

import com.graduration.Constain.RoleConstain;
import com.graduration.Constain.StatusConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRoleResponse {
    String userId;
    String userName;
    String fullName;
    String userCode;
    String userType;
    StatusConstain status;

    @Builder.Default
    Set<RoleConstain> roles = new HashSet<>();
}

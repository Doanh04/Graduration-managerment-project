package com.graduration.DTO.Response;

import com.graduration.Constain.PermissionConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionResponse {
    PermissionConstain permissionId;
    String permissionName;
    String description;
}

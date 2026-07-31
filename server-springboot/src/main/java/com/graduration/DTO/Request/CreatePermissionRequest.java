package com.graduration.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.graduration.Constain.PermissionConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePermissionRequest {
    @NotNull(message = "PERMISSION_NOT_BLANK")
    PermissionConstain permissionId;

    @NotBlank(message = "PERMISSION_NOT_BLANK")
    String permissionName;

    String description;
}

package com.graduration.DTO.Request;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLogRequest {
    String userId;
    String userName;

    @NotBlank(message = "AUDIT_LOG_ACTION_NOT_BLANK")
    String action;

    String resourceType;
    String resourceId;
    String description;
    String ipAddress;

    @Builder.Default
    Map<String, Object> metadata = new HashMap<>();
}

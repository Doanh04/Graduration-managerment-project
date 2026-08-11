package com.graduration.DTO.Response;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLogResponse {
    String auditLogId;
    String userId;
    String userName;
    String action;
    String resourceType;
    String resourceId;
    String description;
    String ipAddress;

    @Builder.Default
    Map<String, Object> metadata = new HashMap<>();

    Instant createdAt;
}

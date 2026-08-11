package com.graduration.entity;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Document(collection = "AuditLog")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLogDocument {
    @Id
    String auditLogId;

    @Indexed
    String userId;

    String userName;

    @Indexed
    String action;

    String resourceType;
    String resourceId;
    String description;
    String ipAddress;

    @Builder.Default
    Map<String, Object> metadata = new HashMap<>();

    @Indexed
    Instant createdAt;
}

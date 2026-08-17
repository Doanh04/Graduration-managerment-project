package com.graduration.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.graduration.Constain.NotificationTypeConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Document(collection = "notification")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationDocument {
    @Id
    String idNotification;

    String userId;
    String title;
    String content;
    NotificationTypeConstain type;

    @Builder.Default
    Boolean read = false;

    Instant createdAt;
    Instant readAt;
    String relatedResourceType;
    String relatedResourceId;
    String deliveryStatus;
}

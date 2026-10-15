package com.graduration.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.graduration.Constain.NotificationTypeConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Document(collection = "notification")
@CompoundIndexes({
    @CompoundIndex(name = "idx_notification_user_created", def = "{'userId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "idx_notification_user_read", def = "{'userId': 1, 'read': 1}")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationDocument {
    @Id
    String idNotification;

    @Indexed
    String userId;
    String title;
    String content;
    NotificationTypeConstain type;

    @Builder.Default
    Boolean read = false;

    @Indexed
    Instant createdAt;
    Instant readAt;
    String relatedResourceType;
    String relatedResourceId;
    String deliveryStatus;
}

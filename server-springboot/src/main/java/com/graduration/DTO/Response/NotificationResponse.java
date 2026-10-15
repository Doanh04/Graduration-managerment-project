package com.graduration.DTO.Response;

import java.time.Instant;

import com.graduration.Constain.NotificationTypeConstain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    String idNotification;
    String title;
    String content;
    NotificationTypeConstain type;
    Boolean read;
    Instant createdAt;
    Instant readAt;
    String relatedResourceType;
    String relatedResourceId;
}

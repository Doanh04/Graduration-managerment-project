package com.graduration.Service.NotificationService;

import java.util.Set;

import com.graduration.Constain.NotificationTypeConstain;
import com.graduration.Constain.RoleConstain;

public record NotificationEvent(
        Set<String> recipientUserIds,
        Set<RoleConstain> recipientRoles,
        String title,
        String content,
        NotificationTypeConstain type,
        String relatedResourceType,
        String relatedResourceId,
        boolean uniquePerRecipientAndResource) {}

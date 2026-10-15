package com.graduration.Service.NotificationService;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.NotificationTypeConstain;
import com.graduration.Constain.RoleConstain;
import com.graduration.DTO.Response.NotificationResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.Repository.NotificationRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.NotificationDocument;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Chỉ phát sự kiện nhẹ trong giao dịch nghiệp vụ. Dữ liệu MongoDB được ghi
     * sau khi giao dịch MySQL commit để thông báo không thể làm rollback nghiệp vụ.
     */
    public void notifyUsers(
            Collection<String> userIds,
            String title,
            String content,
            NotificationTypeConstain type,
            String resourceType,
            Object resourceId) {
        publish(userIds, Set.of(), title, content, type, resourceType, resourceId, false);
    }

    public void notifyUsersOnce(
            Collection<String> userIds,
            String title,
            String content,
            NotificationTypeConstain type,
            String resourceType,
            Object resourceId) {
        publish(userIds, Set.of(), title, content, type, resourceType, resourceId, true);
    }

    public void notifyRoles(
            Collection<RoleConstain> roles,
            String title,
            String content,
            NotificationTypeConstain type,
            String resourceType,
            Object resourceId) {
        publish(Set.of(), roles, title, content, type, resourceType, resourceId, false);
    }

    private void publish(
            Collection<String> userIds,
            Collection<RoleConstain> roles,
            String title,
            String content,
            NotificationTypeConstain type,
            String resourceType,
            Object resourceId,
            boolean unique) {
        Set<String> normalizedUsers = new LinkedHashSet<>();
        if (userIds != null) {
            userIds.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .forEach(normalizedUsers::add);
        }
        Set<RoleConstain> normalizedRoles = roles == null ? Set.of() : new LinkedHashSet<>(roles);
        if (normalizedUsers.isEmpty() && normalizedRoles.isEmpty()) return;
        try {
            eventPublisher.publishEvent(new NotificationEvent(
                    Set.copyOf(normalizedUsers),
                    Set.copyOf(normalizedRoles),
                    title,
                    content,
                    type == null ? NotificationTypeConstain.SYSTEM : type,
                    resourceType,
                    resourceId == null ? null : String.valueOf(resourceId),
                    unique));
        } catch (Exception exception) {
            log.warn("Không thể phát sự kiện thông báo {}: {}", type, exception.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void deliverAfterCommit(NotificationEvent event) {
        try {
            Set<String> recipients = new LinkedHashSet<>(event.recipientUserIds());
            if (!event.recipientRoles().isEmpty()) {
                userRepository.findDistinctByRoles_RoleIn(event.recipientRoles()).stream()
                        .map(user -> user.getUserId())
                        .filter(value -> value != null && !value.isBlank())
                        .forEach(recipients::add);
            }
            Instant now = Instant.now();
            for (String userId : recipients) {
                if (event.uniquePerRecipientAndResource()
                        && notificationRepository.existsByUserIdAndTypeAndRelatedResourceTypeAndRelatedResourceId(
                                userId, event.type(), event.relatedResourceType(), event.relatedResourceId())) {
                    continue;
                }
                notificationRepository.save(NotificationDocument.builder()
                        .userId(userId)
                        .title(event.title())
                        .content(event.content())
                        .type(event.type())
                        .read(false)
                        .createdAt(now)
                        .relatedResourceType(event.relatedResourceType())
                        .relatedResourceId(event.relatedResourceId())
                        .deliveryStatus("DELIVERED")
                        .build());
            }
        } catch (Exception exception) {
            // Thông báo là chức năng phụ trợ: MongoDB lỗi không được ảnh hưởng nghiệp vụ chính.
            log.warn("Không thể lưu thông báo {}: {}", event.type(), exception.getMessage());
        }
    }

    @PreAuthorize("isAuthenticated()")
    public PageResponse<NotificationResponse> getMine(Integer page, Integer size) {
        return PageResponse.from(
                notificationRepository.findByUserIdOrderByCreatedAtDesc(
                        currentUserId(),
                        PaginationSupport.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))),
                this::toResponse);
    }

    @PreAuthorize("isAuthenticated()")
    public long unreadCount() {
        return notificationRepository.countByUserIdAndReadFalse(currentUserId());
    }

    @PreAuthorize("isAuthenticated()")
    public NotificationResponse markRead(String notificationId) {
        NotificationDocument notification = notificationRepository
                .findByIdNotificationAndUserId(notificationId, currentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!Boolean.TRUE.equals(notification.getRead())) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notification.setDeliveryStatus("READ");
            notification = notificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    @PreAuthorize("isAuthenticated()")
    public long markAllRead() {
        var notifications = notificationRepository.findByUserIdAndReadFalse(currentUserId());
        Instant now = Instant.now();
        notifications.forEach(notification -> {
            notification.setRead(true);
            notification.setReadAt(now);
            notification.setDeliveryStatus("READ");
        });
        if (!notifications.isEmpty()) notificationRepository.saveAll(notifications);
        return notifications.size();
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication.getName();
    }

    private NotificationResponse toResponse(NotificationDocument notification) {
        return NotificationResponse.builder()
                .idNotification(notification.getIdNotification())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .read(notification.getRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .relatedResourceType(notification.getRelatedResourceType())
                .relatedResourceId(notification.getRelatedResourceId())
                .build();
    }
}

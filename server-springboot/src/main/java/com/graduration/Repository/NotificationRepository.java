package com.graduration.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.graduration.Constain.NotificationTypeConstain;
import com.graduration.entity.NotificationDocument;

public interface NotificationRepository extends MongoRepository<NotificationDocument, String> {
    Page<NotificationDocument> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByUserIdAndReadFalse(String userId);

    Optional<NotificationDocument> findByIdNotificationAndUserId(String notificationId, String userId);

    List<NotificationDocument> findByUserIdAndReadFalse(String userId);

    boolean existsByUserIdAndTypeAndRelatedResourceTypeAndRelatedResourceId(
            String userId,
            NotificationTypeConstain type,
            String relatedResourceType,
            String relatedResourceId);
}

package com.graduration.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.graduration.entity.AuditLogDocument;

public interface AuditLogRepository extends MongoRepository<AuditLogDocument, String> {
    Page<AuditLogDocument> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<AuditLogDocument> findByActionIgnoreCaseOrderByCreatedAtDesc(String action, Pageable pageable);
}

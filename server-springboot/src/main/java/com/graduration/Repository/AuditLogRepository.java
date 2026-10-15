package com.graduration.Repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.graduration.entity.AuditLogDocument;

public interface AuditLogRepository extends MongoRepository<AuditLogDocument, String> {
    Page<AuditLogDocument> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<AuditLogDocument> findByActionIgnoreCaseOrderByCreatedAtDesc(String action, Pageable pageable);

    List<AuditLogDocument> findAllByOrderByCreatedAtAsc(Pageable pageable);

    @Query(value = "{ 'createdAt': { $gte: ?0, $lt: ?1 } }", sort = "{ 'createdAt': -1 }")
    List<AuditLogDocument> findForExport(Instant from, Instant toExclusive);
}

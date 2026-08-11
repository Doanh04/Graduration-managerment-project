package com.graduration.Service.AuditService;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.DTO.Response.AuditLogResponse;
import com.graduration.Repository.AuditLogRepository;
import com.graduration.entity.AuditLogDocument;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.AuditLogMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditLogService {
    AuditLogRepository auditLogRepository;
    AuditLogMapper auditLogMapper;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AuditLogResponse getAuditLog(String auditLogId) {
        return auditLogMapper.toAuditLogResponse(findAuditLog(auditLogId));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<AuditLogResponse> getAllAuditLogs() {
        return getAllAuditLogs(0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<AuditLogResponse> getAllAuditLogs(Integer page, Integer size) {
        return mapToResponses(auditLogRepository
                .findAll(PaginationSupport.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<AuditLogResponse> getAuditLogsByUserId(String userId) {
        return getAuditLogsByUserId(userId, 0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<AuditLogResponse> getAuditLogsByUserId(String userId, Integer page, Integer size) {
        if (userId == null || userId.isBlank()) {
            throw new AppException(ErrorCode.AUDIT_LOG_USER_ID_NOT_BLANK);
        }
        return mapToResponses(auditLogRepository
                .findByUserIdOrderByCreatedAtDesc(userId.trim(), PaginationSupport.pageRequest(page, size))
                .getContent());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<AuditLogResponse> getAuditLogsByAction(String action) {
        return getAuditLogsByAction(action, 0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<AuditLogResponse> getAuditLogsByAction(String action, Integer page, Integer size) {
        if (action == null || action.isBlank()) {
            throw new AppException(ErrorCode.AUDIT_LOG_ACTION_NOT_BLANK);
        }
        return mapToResponses(auditLogRepository
                .findByActionIgnoreCaseOrderByCreatedAtDesc(action.trim(), PaginationSupport.pageRequest(page, size))
                .getContent());
    }

    private AuditLogDocument findAuditLog(String auditLogId) {
        if (auditLogId == null || auditLogId.isBlank()) {
            throw new AppException(ErrorCode.AUDIT_LOG_NOT_FOUND);
        }
        return auditLogRepository
                .findById(auditLogId.trim())
                .orElseThrow(() -> new AppException(ErrorCode.AUDIT_LOG_NOT_FOUND));
    }

    private List<AuditLogResponse> mapToResponses(List<AuditLogDocument> auditLogs) {
        return auditLogs.stream().map(auditLogMapper::toAuditLogResponse).toList();
    }
}

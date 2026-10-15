package com.graduration.Service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.DTO.Request.UpdateSystemSettingRequest;
import com.graduration.DTO.Response.SystemSettingResponse;
import com.graduration.Repository.AuditLogRepository;
import com.graduration.Repository.SystemSettingRepository;
import com.graduration.entity.AuditLogDocument;
import com.graduration.entity.SystemSettingEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SystemSettingService {
    private final SystemSettingRepository systemSettingRepository;
    private final AuditLogRepository auditLogRepository;

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public SystemSettingResponse getSettings() {
        return toResponse(getOrCreate());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    public SystemSettingResponse updateSettings(UpdateSystemSettingRequest request) {
        SystemSettingEntity settings = getOrCreate();
        settings.setStudentTopicProposalEnabled(request.getStudentTopicProposalEnabled());
        settings.setAuditRetentionLimit(request.getAuditRetentionLimit());
        settings.setLibraryTopicsVisible(request.getLibraryTopicsVisible());
        settings.setTemplatesVisible(request.getTemplatesVisible());
        SystemSettingEntity saved = systemSettingRepository.save(settings);
        enforceAuditRetention(saved.getAuditRetentionLimit());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public void requireStudentTopicProposalEnabled() {
        if (!findSettings().isStudentTopicProposalEnabled()) {
            throw new AppException(ErrorCode.STUDENT_TOPIC_PROPOSAL_DISABLED);
        }
    }

    @Transactional(readOnly = true)
    public boolean canViewLibraryTopics() {
        return isManagementUser() || findSettings().isLibraryTopicsVisible();
    }

    @Transactional(readOnly = true)
    public boolean canViewTemplates() {
        return isManagementUser() || findSettings().isTemplatesVisible();
    }

    public void requireLibraryTopicsVisible() {
        if (!canViewLibraryTopics()) {
            throw new AppException(ErrorCode.LIBRARY_TOPICS_HIDDEN);
        }
    }

    public void requireTemplatesVisible() {
        if (!canViewTemplates()) {
            throw new AppException(ErrorCode.TEMPLATES_HIDDEN);
        }
    }

    private SystemSettingEntity getOrCreate() {
        return systemSettingRepository
                .findById(SystemSettingEntity.SINGLETON_ID)
                .orElseGet(() -> systemSettingRepository.save(defaultSettings()));
    }

    private SystemSettingEntity findSettings() {
        return systemSettingRepository
                .findById(SystemSettingEntity.SINGLETON_ID)
                .orElseGet(this::defaultSettings);
    }

    private SystemSettingEntity defaultSettings() {
        return SystemSettingEntity.builder()
                .settingId(SystemSettingEntity.SINGLETON_ID)
                .build();
    }

    private boolean isManagementUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                                || "ROLE_FACULTY".equals(authority.getAuthority()));
    }

    private void enforceAuditRetention(int retentionLimit) {
        long excess = auditLogRepository.count() - retentionLimit;
        if (excess <= 0) {
            return;
        }
        List<String> ids =
                auditLogRepository
                        .findAllByOrderByCreatedAtAsc(PageRequest.of(0, (int) Math.min(excess, Integer.MAX_VALUE)))
                        .stream()
                        .map(AuditLogDocument::getAuditLogId)
                        .toList();
        auditLogRepository.deleteAllById(ids);
    }

    private SystemSettingResponse toResponse(SystemSettingEntity entity) {
        return SystemSettingResponse.builder()
                .studentTopicProposalEnabled(entity.isStudentTopicProposalEnabled())
                .auditRetentionLimit(entity.getAuditRetentionLimit())
                .libraryTopicsVisible(entity.isLibraryTopicsVisible())
                .templatesVisible(entity.isTemplatesVisible())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

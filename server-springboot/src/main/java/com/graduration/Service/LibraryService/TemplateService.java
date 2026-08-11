package com.graduration.Service.LibraryService;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.DTO.Request.TemplateRequest;
import com.graduration.DTO.Response.TemplateResponse;
import com.graduration.Repository.TemplateRepository;
import com.graduration.entity.TemplateEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.TemplateMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TemplateService {
    TemplateRepository templateRepository;
    TemplateMapper templateMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public TemplateResponse createTemplate(TemplateRequest request) {
        normalize(request);
        if (templateRepository.existsByTemplateNameIgnoreCase(request.getTemplateName())) {
            throw new AppException(ErrorCode.TEMPLATE_ALREADY_EXISTS);
        }

        TemplateEntity template = templateMapper.toTemplateEntity(request);
        template.setCreateAt(LocalDate.now());
        return templateMapper.toTemplateResponse(templateRepository.save(template));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public TemplateResponse getTemplate(Integer templateId) {
        return templateMapper.toTemplateResponse(findTemplate(templateId));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<TemplateResponse> getAllTemplates() {
        return getAllTemplates(0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<TemplateResponse> getAllTemplates(Integer page, Integer size) {
        return templateRepository.findAll(PaginationSupport.pageRequest(page, size)).stream()
                .map(templateMapper::toTemplateResponse)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public TemplateResponse updateTemplate(Integer templateId, TemplateRequest request) {
        TemplateEntity template = findTemplate(templateId);
        normalize(request);
        if (templateRepository.existsByTemplateNameIgnoreCaseAndTemplateIdNot(request.getTemplateName(), templateId)) {
            throw new AppException(ErrorCode.TEMPLATE_ALREADY_EXISTS);
        }

        templateMapper.updateTemplate(request, template);
        return templateMapper.toTemplateResponse(templateRepository.save(template));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public void deleteTemplate(Integer templateId) {
        templateRepository.delete(findTemplate(templateId));
    }

    private TemplateEntity findTemplate(Integer templateId) {
        if (templateId == null) {
            throw new AppException(ErrorCode.TEMPLATE_NOT_FOUND);
        }
        return templateRepository
                .findById(templateId)
                .orElseThrow(() -> new AppException(ErrorCode.TEMPLATE_NOT_FOUND));
    }

    private void normalize(TemplateRequest request) {
        if (request == null
                || request.getTemplateName() == null
                || request.getTemplateName().isBlank()) {
            throw new AppException(ErrorCode.TEMPLATE_NAME_NOT_BLANK);
        }
        request.setTemplateName(request.getTemplateName().trim());
        request.setDescription(normalizeNullable(request.getDescription()));
        request.setFilePath(normalizeNullable(request.getFilePath()));
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

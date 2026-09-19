package com.graduration.Service.LibraryService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.TemplateTypeConstain;
import com.graduration.DTO.Request.TemplateRequest;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.TemplateResponse;
import com.graduration.Repository.TemplateRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.TemplateEntity;
import com.graduration.entity.UserEntity;
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
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");

    TemplateRepository templateRepository;
    UserRepository userRepository;
    TemplateMapper templateMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public TemplateResponse createTemplate(TemplateRequest request, MultipartFile file) {
        validateRequest(request);
        validateFile(file);
        if (templateRepository.existsByTemplateNameIgnoreCase(
                request.getTemplateName().trim())) {
            throw new AppException(ErrorCode.TEMPLATE_ALREADY_EXISTS);
        }

        TemplateEntity template = new TemplateEntity();
        template.setTemplateName(request.getTemplateName().trim());
        template.setDescription(normalizeNullable(request.getDescription()));
        template.setTemplateType(normalizeTemplateType(request.getTemplateType()));
        template.setOriginalFileName(safeFileName(file.getOriginalFilename()));
        template.setContentType(file.getContentType());
        template.setFileSize(file.getSize());
        template.setFileData(encodeFile(file));
        template.setCreateAt(LocalDate.now());
        template.setUploadedBy(currentUser());
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

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public PageResponse<TemplateResponse> getAllTemplatesPage(Integer page, Integer size) {
        return getAllTemplatesPage(page, size, null);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public PageResponse<TemplateResponse> getAllTemplatesPage(
            Integer page, Integer size, TemplateTypeConstain templateType) {
        Page<TemplateEntity> templates = templateType == null
                ? templateRepository.findAll(PaginationSupport.pageRequest(page, size))
                : templateRepository.findByTemplateType(templateType, PaginationSupport.pageRequest(page, size));
        return PageResponse.from(templates, templateMapper::toTemplateResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public TemplateResponse updateTemplate(Integer templateId, TemplateRequest request, MultipartFile file) {
        TemplateEntity template = findTemplate(templateId);
        validateRequest(request);
        String name = request.getTemplateName().trim();
        if (templateRepository.existsByTemplateNameIgnoreCaseAndTemplateIdNot(name, templateId)) {
            throw new AppException(ErrorCode.TEMPLATE_ALREADY_EXISTS);
        }

        if (file != null && !file.isEmpty()) {
            validateFile(file);
        }
        template.setTemplateName(name);
        template.setDescription(normalizeNullable(request.getDescription()));
        if (request.getTemplateType() != null) {
            template.setTemplateType(request.getTemplateType());
        } else if (template.getTemplateType() == null) {
            template.setTemplateType(TemplateTypeConstain.OTHER);
        }
        if (file != null && !file.isEmpty()) {
            template.setOriginalFileName(safeFileName(file.getOriginalFilename()));
            template.setContentType(file.getContentType());
            template.setFileSize(file.getSize());
            template.setFileData(encodeFile(file));
        }
        return templateMapper.toTemplateResponse(templateRepository.save(template));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public void deleteTemplate(Integer templateId) {
        TemplateEntity template = findTemplate(templateId);
        templateRepository.delete(template);
        templateRepository.flush();
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public DownloadedTemplate downloadTemplate(Integer templateId) {
        TemplateEntity template = findTemplate(templateId);
        byte[] content = template.getFileData();
        if (content == null || content.length == 0) {
            throw new AppException(ErrorCode.FILE_STORAGE_ERROR);
        }
        return new DownloadedTemplate(
                new ByteArrayResource(content),
                safeFileName(template.getOriginalFileName()),
                template.getContentType());
    }

    private TemplateEntity findTemplate(Integer templateId) {
        if (templateId == null) {
            throw new AppException(ErrorCode.TEMPLATE_NOT_FOUND);
        }
        return templateRepository
                .findById(templateId)
                .orElseThrow(() -> new AppException(ErrorCode.TEMPLATE_NOT_FOUND));
    }

    private UserEntity currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return userRepository
                .findById(authentication.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateRequest(TemplateRequest request) {
        if (request == null
                || request.getTemplateName() == null
                || request.getTemplateName().isBlank()) {
            throw new AppException(ErrorCode.TEMPLATE_NAME_NOT_BLANK);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.TEMPLATE_FILE_REQUIRED);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AppException(ErrorCode.TEMPLATE_FILE_TOO_LARGE);
        }
        if (!ALLOWED_EXTENSIONS.contains(extension(file.getOriginalFilename()))) {
            throw new AppException(ErrorCode.TEMPLATE_FILE_TYPE_NOT_ALLOWED);
        }
    }

    private String extension(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String safeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "bieu-mau";
        }
        return name.replace('\\', '/').substring(name.replace('\\', '/').lastIndexOf('/') + 1);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private TemplateTypeConstain normalizeTemplateType(TemplateTypeConstain value) {
        return value == null ? TemplateTypeConstain.OTHER : value;
    }

    /** Đọc toàn bộ nội dung MultipartFile để lưu nguyên bản vào cột BLOB file_data của database. */
    private byte[] encodeFile(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new AppException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    public record DownloadedTemplate(Resource resource, String fileName, String contentType) {}
}

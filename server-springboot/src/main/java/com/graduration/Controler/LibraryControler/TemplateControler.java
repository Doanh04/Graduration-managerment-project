package com.graduration.Controler.LibraryControler;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.graduration.Constain.TemplateTypeConstain;
import com.graduration.DTO.Request.TemplateRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.TemplateResponse;
import com.graduration.Service.LibraryService.TemplateService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/template")
@RequiredArgsConstructor
public class TemplateControler {
    private final TemplateService templateService;

    @PostMapping(value = "/create-template", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TemplateResponse> createTemplate(
            @RequestParam String templateName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) TemplateTypeConstain templateType,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.<TemplateResponse>builder()
                .message("Template created successfully")
                .result(templateService.createTemplate(
                        new TemplateRequest(templateName, description, templateType), file))
                .build();
    }

    @GetMapping("/{templateId}")
    public ApiResponse<TemplateResponse> getTemplate(@PathVariable Integer templateId) {
        return ApiResponse.<TemplateResponse>builder()
                .result(templateService.getTemplate(templateId))
                .build();
    }

    @GetMapping("/get-all-template")
    public ApiResponse<com.graduration.DTO.Response.PageResponse<TemplateResponse>> getAllTemplates(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer size,
            @org.springframework.web.bind.annotation.RequestParam(required = false) TemplateTypeConstain templateType) {
        return ApiResponse.<com.graduration.DTO.Response.PageResponse<TemplateResponse>>builder()
                .result(templateService.getAllTemplatesPage(page, size, templateType))
                .build();
    }

    @PutMapping(value = "/{templateId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TemplateResponse> updateTemplate(
            @PathVariable Integer templateId,
            @RequestParam String templateName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) TemplateTypeConstain templateType,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ApiResponse.<TemplateResponse>builder()
                .message("Template updated successfully")
                .result(templateService.updateTemplate(
                        templateId, new TemplateRequest(templateName, description, templateType), file))
                .build();
    }

    @GetMapping("/{templateId}/file")
    public ResponseEntity<Resource> downloadTemplate(@PathVariable Integer templateId) {
        TemplateService.DownloadedTemplate file = templateService.downloadTemplate(templateId);
        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            if (file.contentType() != null) {
                contentType = MediaType.parseMediaType(file.contentType());
            }
        } catch (IllegalArgumentException ignored) {
            // Tệp vẫn được trả về với kiểu nhị phân nếu metadata cũ không hợp lệ.
        }
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.resource());
    }

    @DeleteMapping("/{templateId}")
    public ApiResponse<Void> deleteTemplate(@PathVariable Integer templateId) {
        templateService.deleteTemplate(templateId);
        return ApiResponse.<Void>builder()
                .message("Template deleted successfully")
                .build();
    }
}

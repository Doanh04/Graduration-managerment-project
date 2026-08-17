package com.graduration.Controler.LibraryControler;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/create-template")
    public ApiResponse<TemplateResponse> createTemplate(@Valid @RequestBody TemplateRequest request) {
        return ApiResponse.<TemplateResponse>builder()
                .message("Template created successfully")
                .result(templateService.createTemplate(request))
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
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer size) {
        return ApiResponse.<com.graduration.DTO.Response.PageResponse<TemplateResponse>>builder()
                .result(templateService.getAllTemplatesPage(page, size))
                .build();
    }

    @PutMapping("/{templateId}")
    public ApiResponse<TemplateResponse> updateTemplate(
            @PathVariable Integer templateId, @Valid @RequestBody TemplateRequest request) {
        return ApiResponse.<TemplateResponse>builder()
                .message("Template updated successfully")
                .result(templateService.updateTemplate(templateId, request))
                .build();
    }

    @DeleteMapping("/{templateId}")
    public ApiResponse<Void> deleteTemplate(@PathVariable Integer templateId) {
        templateService.deleteTemplate(templateId);
        return ApiResponse.<Void>builder()
                .message("Template deleted successfully")
                .build();
    }
}

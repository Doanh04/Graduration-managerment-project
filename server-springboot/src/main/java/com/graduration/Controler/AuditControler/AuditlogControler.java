package com.graduration.Controler.AuditControler;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.AuditLogResponse;
import com.graduration.Service.AuditService.AuditLogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/audit-log")
@RequiredArgsConstructor
public class AuditlogControler {
    private final AuditLogService auditLogService;

    @GetMapping("/{auditLogId}")
    public ApiResponse<AuditLogResponse> getAuditLog(@PathVariable String auditLogId) {
        return ApiResponse.<AuditLogResponse>builder()
                .result(auditLogService.getAuditLog(auditLogId))
                .build();
    }

    @GetMapping("/get-all-audit-log")
    public ApiResponse<com.graduration.DTO.Response.PageResponse<AuditLogResponse>> getAllAuditLogs(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer size) {
        return ApiResponse.<com.graduration.DTO.Response.PageResponse<AuditLogResponse>>builder()
                .result(auditLogService.getAllAuditLogsPage(page, size))
                .build();
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<AuditLogResponse>> getAuditLogsByUserId(
            @PathVariable String userId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer size) {
        return ApiResponse.<List<AuditLogResponse>>builder()
                .result(
                        page == null && size == null
                                ? auditLogService.getAuditLogsByUserId(userId)
                                : auditLogService.getAuditLogsByUserId(userId, page, size))
                .build();
    }

    @GetMapping("/action/{action}")
    public ApiResponse<List<AuditLogResponse>> getAuditLogsByAction(
            @PathVariable String action,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer size) {
        return ApiResponse.<List<AuditLogResponse>>builder()
                .result(
                        page == null && size == null
                                ? auditLogService.getAuditLogsByAction(action)
                                : auditLogService.getAuditLogsByAction(action, page, size))
                .build();
    }
}

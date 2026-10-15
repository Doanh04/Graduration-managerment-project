package com.graduration.Controler.AuditControler;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAuditLogs(
            @org.springframework.web.bind.annotation.RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fromDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate toDate) {
        byte[] content = auditLogService.exportAuditLogs(fromDate, toDate);
        String fileName = "nhat-ky-hoat-dong_" + fromDate + "_den_" + toDate + ".xlsx";
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(content.length)
                .body(content);
    }
}

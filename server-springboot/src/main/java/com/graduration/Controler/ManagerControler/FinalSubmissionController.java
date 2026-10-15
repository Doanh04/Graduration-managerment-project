package com.graduration.Controler.ManagerControler;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.graduration.DTO.Response.ApiResponse;
import com.graduration.Service.ManagerService.FinalSubmissionService;
import com.graduration.Service.ManagerService.FinalSubmissionService.Decision;
import com.graduration.Service.ManagerService.FinalSubmissionService.Overview;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/final-submissions")
@RequiredArgsConstructor
public class FinalSubmissionController {
    private final FinalSubmissionService service;

    @GetMapping("/mine")
    public ApiResponse<List<Overview>> mine() {
        return ApiResponse.<List<Overview>>builder().result(service.mine()).build();
    }

    @GetMapping("/admin")
    public ApiResponse<List<Overview>> adminList() {
        return ApiResponse.<List<Overview>>builder().result(service.adminList()).build();
    }

    @GetMapping("/topic/{topicId}")
    public ApiResponse<Overview> get(@PathVariable Long topicId) {
        return ApiResponse.<Overview>builder().result(service.get(topicId)).build();
    }

    @PostMapping(value = "/topic/{topicId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Overview> submit(
            @PathVariable Long topicId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String sourceUrl,
            @RequestParam(required = false) String productUrl,
            @RequestParam(required = false) String note) {
        return ApiResponse.<Overview>builder()
                .message("Đã nộp bản final và chuyển Admin duyệt")
                .result(service.submit(topicId, file, sourceUrl, productUrl, note))
                .build();
    }

    @PostMapping("/topic/{topicId}/review")
    public ApiResponse<Overview> review(@PathVariable Long topicId, @RequestBody Decision decision) {
        return ApiResponse.<Overview>builder()
                .message(Boolean.TRUE.equals(decision.approved())
                        ? "Đã duyệt bản final và công bố điểm"
                        : "Đã yêu cầu sinh viên nộp lại")
                .result(service.review(topicId, decision))
                .build();
    }

    @GetMapping("/{submissionId}/file")
    public ResponseEntity<byte[]> file(@PathVariable Long submissionId) {
        var file = service.file(submissionId);
        ContentDisposition disposition = ("application/pdf".equals(file.type())
                ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(file.name(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.type()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(file.bytes());
    }

    @GetMapping("/admin/backup/{defensePeriodId}")
    public ResponseEntity<byte[]> backup(@PathVariable Long defensePeriodId) {
        var archive = service.backup(defensePeriodId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(archive.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(archive.bytes().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(archive.bytes());
    }
}

package com.graduration.Controler.GradurationControler;

import java.nio.charset.StandardCharsets;

import jakarta.validation.Valid;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.graduration.Constain.SubmissionStatusConstain;
import com.graduration.DTO.Request.SubmissionCommentRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.SubmissionResponse;
import com.graduration.Service.GradurationService.SubmissionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class SubmissionControler {
    private final SubmissionService submissionService;

    @PostMapping(value = "/milestones/{milestoneId}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SubmissionResponse> upload(
            @PathVariable Long milestoneId,
            @RequestParam Long teamId,
            @RequestParam(required = false) String note,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.<SubmissionResponse>builder()
                .message("Submission uploaded successfully")
                .result(submissionService.upload(milestoneId, teamId, note, file, null))
                .build();
    }

    @GetMapping("/submissions")
    public ApiResponse<PageResponse<SubmissionResponse>> getSubmissions(
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long milestoneId,
            @RequestParam(required = false) SubmissionStatusConstain status,
            @RequestParam(required = false) Boolean late,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<SubmissionResponse>>builder()
                .result(submissionService.getSubmissions(teamId, milestoneId, status, late, page, size))
                .build();
    }

    @GetMapping("/teams/{teamId}/submissions")
    public ApiResponse<PageResponse<SubmissionResponse>> getTeamSubmissions(
            @PathVariable Long teamId,
            @RequestParam(required = false) Long milestoneId,
            @RequestParam(required = false) SubmissionStatusConstain status,
            @RequestParam(required = false) Boolean late,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<SubmissionResponse>>builder()
                .result(submissionService.getSubmissions(teamId, milestoneId, status, late, page, size))
                .build();
    }

    @GetMapping("/teams/{teamId}/milestones/{milestoneId}/submissions")
    public ApiResponse<PageResponse<SubmissionResponse>> getVersionHistory(
            @PathVariable Long teamId,
            @PathVariable Long milestoneId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<SubmissionResponse>>builder()
                .result(submissionService.getVersionHistory(teamId, milestoneId, page, size))
                .build();
    }

    @GetMapping("/submissions/{submissionId}")
    public ApiResponse<SubmissionResponse> getSubmission(@PathVariable Long submissionId) {
        return ApiResponse.<SubmissionResponse>builder()
                .result(submissionService.getSubmission(submissionId))
                .build();
    }

    @GetMapping("/submissions/{submissionId}/file")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long submissionId) {
        SubmissionService.DownloadedSubmission download = submissionService.download(submissionId);
        MediaType contentType;
        try {
            contentType = download.contentType() == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(download.contentType());
        } catch (IllegalArgumentException ignored) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.resource());
    }

    @DeleteMapping("/submissions/{submissionId}")
    public ApiResponse<SubmissionResponse> withdraw(@PathVariable Long submissionId) {
        return ApiResponse.<SubmissionResponse>builder()
                .message("Submission withdrawn successfully")
                .result(submissionService.withdraw(submissionId))
                .build();
    }

    @PatchMapping("/submissions/{submissionId}/review")
    public ApiResponse<SubmissionResponse> startReview(@PathVariable Long submissionId) {
        return ApiResponse.<SubmissionResponse>builder()
                .message("Submission review started")
                .result(submissionService.startReview(submissionId))
                .build();
    }

    @PatchMapping("/submissions/{submissionId}/request-revision")
    public ApiResponse<SubmissionResponse> requestRevision(
            @PathVariable Long submissionId, @Valid @RequestBody SubmissionCommentRequest request) {
        return reviewResponse(
                "Submission revision requested", submissionService.requestRevision(submissionId, request.getComment()));
    }

    @PatchMapping("/submissions/{submissionId}/approve")
    public ApiResponse<SubmissionResponse> approve(
            @PathVariable Long submissionId, @Valid @RequestBody SubmissionCommentRequest request) {
        return reviewResponse(
                "Submission approved successfully", submissionService.approve(submissionId, request.getComment()));
    }

    @PatchMapping("/submissions/{submissionId}/reject")
    public ApiResponse<SubmissionResponse> reject(
            @PathVariable Long submissionId, @Valid @RequestBody SubmissionCommentRequest request) {
        return reviewResponse("Submission rejected", submissionService.reject(submissionId, request.getComment()));
    }

    private ApiResponse<SubmissionResponse> reviewResponse(String message, SubmissionResponse result) {
        return ApiResponse.<SubmissionResponse>builder()
                .message(message)
                .result(result)
                .build();
    }
}

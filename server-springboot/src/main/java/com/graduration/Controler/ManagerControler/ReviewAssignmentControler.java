package com.graduration.Controler.ManagerControler;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.graduration.Constain.ReviewAssignmentStatusConstain;
import com.graduration.DTO.Request.AssignReviewRequest;
import com.graduration.DTO.Request.CancelReviewRequest;
import com.graduration.DTO.Request.SubmitReviewRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.ReviewAssignmentResponse;
import com.graduration.Service.ManagerService.ReviewAssignmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ReviewAssignmentControler {
    private final ReviewAssignmentService reviewAssignmentService;

    @PostMapping("/topics/{topicId}/review-assignments")
    public ApiResponse<ReviewAssignmentResponse> assign(
            @PathVariable Long topicId, @Valid @RequestBody AssignReviewRequest request) {
        return ApiResponse.<ReviewAssignmentResponse>builder()
                .message("Reviewer assigned successfully")
                .result(reviewAssignmentService.assign(topicId, request))
                .build();
    }

    @GetMapping("/topics/{topicId}/review-assignments")
    public ApiResponse<PageResponse<ReviewAssignmentResponse>> getByTopic(
            @PathVariable Long topicId,
            @RequestParam(required = false) ReviewAssignmentStatusConstain status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<ReviewAssignmentResponse>>builder()
                .result(reviewAssignmentService.getByTopic(topicId, status, page, size))
                .build();
    }

    @GetMapping("/lecturers/{lectureId}/review-assignments")
    public ApiResponse<PageResponse<ReviewAssignmentResponse>> getByLecturer(
            @PathVariable String lectureId,
            @RequestParam(required = false) ReviewAssignmentStatusConstain status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<ReviewAssignmentResponse>>builder()
                .result(reviewAssignmentService.getByLecturer(lectureId, status, page, size))
                .build();
    }

    @GetMapping("/review-assignments/me")
    public ApiResponse<PageResponse<ReviewAssignmentResponse>> getMine(
            @RequestParam(required = false) ReviewAssignmentStatusConstain status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<ReviewAssignmentResponse>>builder()
                .result(reviewAssignmentService.getMine(status, page, size))
                .build();
    }

    @PatchMapping("/review-assignments/{assignmentId}/start")
    public ApiResponse<ReviewAssignmentResponse> start(@PathVariable Long assignmentId) {
        return ApiResponse.<ReviewAssignmentResponse>builder()
                .message("Review started successfully")
                .result(reviewAssignmentService.start(assignmentId))
                .build();
    }

    @PatchMapping("/review-assignments/{assignmentId}/submit")
    public ApiResponse<ReviewAssignmentResponse> submit(
            @PathVariable Long assignmentId, @Valid @RequestBody SubmitReviewRequest request) {
        return ApiResponse.<ReviewAssignmentResponse>builder()
                .message("Review submitted successfully")
                .result(reviewAssignmentService.submit(assignmentId, request))
                .build();
    }

    @PatchMapping("/review-assignments/{assignmentId}/approve")
    public ApiResponse<ReviewAssignmentResponse> approve(@PathVariable Long assignmentId) {
        return ApiResponse.<ReviewAssignmentResponse>builder()
                .message("Review approved successfully")
                .result(reviewAssignmentService.approve(assignmentId))
                .build();
    }

    @PatchMapping("/review-assignments/{assignmentId}/request-revision")
    public ApiResponse<ReviewAssignmentResponse> requestRevision(@PathVariable Long assignmentId) {
        return ApiResponse.<ReviewAssignmentResponse>builder()
                .message("Review revision requested successfully")
                .result(reviewAssignmentService.requestRevision(assignmentId))
                .build();
    }

    @PatchMapping("/review-assignments/{assignmentId}/cancel")
    public ApiResponse<ReviewAssignmentResponse> cancel(
            @PathVariable Long assignmentId, @Valid @RequestBody CancelReviewRequest request) {
        return ApiResponse.<ReviewAssignmentResponse>builder()
                .message("Review assignment cancelled successfully")
                .result(reviewAssignmentService.cancel(assignmentId, request))
                .build();
    }
}

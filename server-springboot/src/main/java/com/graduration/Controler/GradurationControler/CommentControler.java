package com.graduration.Controler.GradurationControler;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.graduration.DTO.Request.SubmissionCommentRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.SubmissionCommentResponse;
import com.graduration.Service.GradurationService.CommentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CommentControler {
    private final CommentService commentService;

    @PostMapping("/submissions/{submissionId}/comments")
    public ApiResponse<SubmissionCommentResponse> addComment(
            @PathVariable Long submissionId, @Valid @RequestBody SubmissionCommentRequest request) {
        return ApiResponse.<SubmissionCommentResponse>builder()
                .message("Comment added successfully")
                .result(commentService.addComment(submissionId, request.getComment()))
                .build();
    }

    @GetMapping("/submissions/{submissionId}/comments")
    public ApiResponse<PageResponse<SubmissionCommentResponse>> getComments(
            @PathVariable Long submissionId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<SubmissionCommentResponse>>builder()
                .result(commentService.getComments(submissionId, page, size))
                .build();
    }

    @PutMapping("/submission-comments/{commentId}")
    public ApiResponse<SubmissionCommentResponse> updateComment(
            @PathVariable Long commentId, @Valid @RequestBody SubmissionCommentRequest request) {
        return ApiResponse.<SubmissionCommentResponse>builder()
                .message("Comment updated successfully")
                .result(commentService.updateComment(commentId, request.getComment()))
                .build();
    }

    @DeleteMapping("/submission-comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ApiResponse.<Void>builder()
                .message("Comment deleted successfully")
                .build();
    }
}

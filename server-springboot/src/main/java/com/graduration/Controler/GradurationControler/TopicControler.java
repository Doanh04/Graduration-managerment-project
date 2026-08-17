package com.graduration.Controler.GradurationControler;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.graduration.Constain.CategoryTopicConstain;
import com.graduration.Constain.TopicStatusConstain;
import com.graduration.DTO.Request.CreateTopicRequest;
import com.graduration.DTO.Request.RejectTopicRequest;
import com.graduration.DTO.Request.UpdateTopicRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.TopicResponse;
import com.graduration.Service.GradurationService.TopicService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/topics")
@RequiredArgsConstructor
public class TopicControler {
    private final TopicService topicService;

    @PostMapping
    public ApiResponse<TopicResponse> createTopic(@Valid @RequestBody CreateTopicRequest request) {
        return ApiResponse.<TopicResponse>builder()
                .message("Topic created successfully")
                .result(topicService.createTopic(request))
                .build();
    }

    @GetMapping("/{topicId}")
    public ApiResponse<TopicResponse> getTopic(@PathVariable Long topicId) {
        return ApiResponse.<TopicResponse>builder()
                .result(topicService.getTopic(topicId))
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<TopicResponse>> getTopics(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer academicYearId,
            @RequestParam(required = false) Long defensePeriodId,
            @RequestParam(required = false) CategoryTopicConstain categoryTopic,
            @RequestParam(required = false) TopicStatusConstain status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.<PageResponse<TopicResponse>>builder()
                .result(topicService.getTopics(
                        page, size, academicYearId, defensePeriodId, categoryTopic, status, keyword))
                .build();
    }

    @PatchMapping("/{topicId}")
    public ApiResponse<TopicResponse> updateTopic(
            @PathVariable Long topicId, @Valid @RequestBody UpdateTopicRequest request) {
        return ApiResponse.<TopicResponse>builder()
                .message("Topic updated successfully")
                .result(topicService.updateTopic(topicId, request))
                .build();
    }

    @DeleteMapping("/{topicId}")
    public ApiResponse<Void> deleteTopic(@PathVariable Long topicId) {
        topicService.deleteTopic(topicId);
        return ApiResponse.<Void>builder().message("Topic deleted successfully").build();
    }

    @PostMapping("/{topicId}/submit-for-approval")
    public ApiResponse<TopicResponse> submitForApproval(@PathVariable Long topicId) {
        return ApiResponse.<TopicResponse>builder()
                .message("Topic submitted for approval")
                .result(topicService.submitForApproval(topicId))
                .build();
    }

    @PostMapping("/{topicId}/approve")
    public ApiResponse<TopicResponse> approveTopic(@PathVariable Long topicId) {
        return ApiResponse.<TopicResponse>builder()
                .message("Topic approved successfully")
                .result(topicService.approveTopic(topicId))
                .build();
    }

    @PostMapping("/{topicId}/reject")
    public ApiResponse<TopicResponse> rejectTopic(
            @PathVariable Long topicId, @Valid @RequestBody RejectTopicRequest request) {
        return ApiResponse.<TopicResponse>builder()
                .message("Topic rejected successfully")
                .result(topicService.rejectTopic(topicId, request.getReason()))
                .build();
    }
}

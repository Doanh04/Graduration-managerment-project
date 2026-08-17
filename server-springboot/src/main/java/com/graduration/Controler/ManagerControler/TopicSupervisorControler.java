package com.graduration.Controler.ManagerControler;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.graduration.DTO.Request.AssignTopicSupervisorRequest;
import com.graduration.DTO.Request.DeactivateTopicSupervisorRequest;
import com.graduration.DTO.Request.UpdateTopicSupervisorRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.TopicSupervisorResponse;
import com.graduration.Service.ManagerService.TopicSupervisorService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TopicSupervisorControler {
    private final TopicSupervisorService topicSupervisorService;

    @PostMapping("/topics/{topicId}/supervisors")
    public ApiResponse<TopicSupervisorResponse> assign(
            @PathVariable Long topicId, @Valid @RequestBody AssignTopicSupervisorRequest request) {
        return ApiResponse.<TopicSupervisorResponse>builder()
                .message("Topic supervisor assigned successfully")
                .result(topicSupervisorService.assign(topicId, request))
                .build();
    }

    @GetMapping("/topics/{topicId}/supervisors")
    public ApiResponse<PageResponse<TopicSupervisorResponse>> getByTopic(
            @PathVariable Long topicId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<TopicSupervisorResponse>>builder()
                .result(topicSupervisorService.getByTopic(topicId, page, size))
                .build();
    }

    @GetMapping("/lecturers/{lectureId}/supervised-topics")
    public ApiResponse<PageResponse<TopicSupervisorResponse>> getByLecturer(
            @PathVariable String lectureId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<TopicSupervisorResponse>>builder()
                .result(topicSupervisorService.getByLecturer(lectureId, page, size))
                .build();
    }

    @GetMapping("/topic-supervisors/me")
    public ApiResponse<PageResponse<TopicSupervisorResponse>> getMine(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<TopicSupervisorResponse>>builder()
                .result(topicSupervisorService.getMine(page, size))
                .build();
    }

    @PatchMapping("/topic-supervisors/{assignmentId}")
    public ApiResponse<TopicSupervisorResponse> update(
            @PathVariable Long assignmentId, @Valid @RequestBody UpdateTopicSupervisorRequest request) {
        return ApiResponse.<TopicSupervisorResponse>builder()
                .message("Topic supervisor assignment updated successfully")
                .result(topicSupervisorService.update(assignmentId, request))
                .build();
    }

    @PatchMapping("/topic-supervisors/{assignmentId}/deactivate")
    public ApiResponse<TopicSupervisorResponse> deactivate(
            @PathVariable Long assignmentId, @Valid @RequestBody DeactivateTopicSupervisorRequest request) {
        return ApiResponse.<TopicSupervisorResponse>builder()
                .message("Topic supervisor assignment deactivated successfully")
                .result(topicSupervisorService.deactivate(assignmentId, request))
                .build();
    }
}

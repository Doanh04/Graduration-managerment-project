package com.graduration.Controler.GradurationControler;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.graduration.Constain.TopicRegistrationStatusConstain;
import com.graduration.DTO.Request.CreateTopicRegistrationRequest;
import com.graduration.DTO.Request.RejectTopicRequest;
import com.graduration.DTO.Response.*;
import com.graduration.Service.GradurationService.TopicRegistrationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/topic-registrations")
@RequiredArgsConstructor
public class TopicRegistrationControler {
    private final TopicRegistrationService service;

    @PostMapping
    public ApiResponse<TopicRegistrationResponse> register(@Valid @RequestBody CreateTopicRegistrationRequest request) {
        return ApiResponse.<TopicRegistrationResponse>builder()
                .message("Đã gửi đăng ký đề tài")
                .result(service.register(request))
                .build();
    }

    @GetMapping("/my")
    public ApiResponse<List<TopicRegistrationResponse>> mine() {
        return ApiResponse.<List<TopicRegistrationResponse>>builder()
                .result(service.getMine())
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<TopicRegistrationResponse>> getAll(
            @RequestParam(required = false) TopicRegistrationStatusConstain status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<TopicRegistrationResponse>>builder()
                .result(service.getAll(status, page, size))
                .build();
    }

    @PatchMapping("/{id}/approve")
    public ApiResponse<TopicRegistrationResponse> approve(@PathVariable Long id) {
        return ApiResponse.<TopicRegistrationResponse>builder()
                .message("Đã duyệt đăng ký đề tài")
                .result(service.approve(id))
                .build();
    }

    @PatchMapping("/{id}/reject")
    public ApiResponse<TopicRegistrationResponse> reject(
            @PathVariable Long id, @Valid @RequestBody RejectTopicRequest request) {
        return ApiResponse.<TopicRegistrationResponse>builder()
                .message("Đã từ chối đăng ký đề tài")
                .result(service.reject(id, request.getReason()))
                .build();
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<TopicRegistrationResponse> cancel(@PathVariable Long id) {
        return ApiResponse.<TopicRegistrationResponse>builder()
                .message("Đã hủy đăng ký đề tài")
                .result(service.cancel(id))
                .build();
    }
}

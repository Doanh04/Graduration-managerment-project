package com.graduration.Controler.GradurationControler;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.graduration.Constain.MilesStoneStatusConstain;
import com.graduration.Constain.MilesStoneTypeConstain;
import com.graduration.DTO.Request.CreateMilestoneRequest;
import com.graduration.DTO.Request.UpdateMilestoneRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.MilestoneResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.Service.GradurationService.MilestoneService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MilestoneControler {
    private final MilestoneService milestoneService;

    @PostMapping("/defense-periods/{defensePeriodId}/milestones")
    public ApiResponse<MilestoneResponse> createMilestone(
            @PathVariable Long defensePeriodId, @Valid @RequestBody CreateMilestoneRequest request) {
        return ApiResponse.<MilestoneResponse>builder()
                .message("Milestone created successfully")
                .result(milestoneService.createMilestone(defensePeriodId, request))
                .build();
    }

    @GetMapping("/defense-periods/{defensePeriodId}/milestones")
    public ApiResponse<PageResponse<MilestoneResponse>> getMilestonesByDefensePeriod(
            @PathVariable Long defensePeriodId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) MilesStoneStatusConstain status,
            @RequestParam(required = false) MilesStoneTypeConstain type,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.<PageResponse<MilestoneResponse>>builder()
                .result(milestoneService.getMilestones(defensePeriodId, page, size, status, type, keyword))
                .build();
    }

    @GetMapping("/milestones")
    public ApiResponse<PageResponse<MilestoneResponse>> getMilestones(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Long defensePeriodId,
            @RequestParam(required = false) MilesStoneStatusConstain status,
            @RequestParam(required = false) MilesStoneTypeConstain type,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.<PageResponse<MilestoneResponse>>builder()
                .result(milestoneService.getMilestones(defensePeriodId, page, size, status, type, keyword))
                .build();
    }

    @GetMapping("/milestones/{milestoneId}")
    public ApiResponse<MilestoneResponse> getMilestone(@PathVariable Long milestoneId) {
        return ApiResponse.<MilestoneResponse>builder()
                .result(milestoneService.getMilestone(milestoneId))
                .build();
    }

    @PutMapping("/milestones/{milestoneId}")
    public ApiResponse<MilestoneResponse> updateMilestone(
            @PathVariable Long milestoneId, @Valid @RequestBody UpdateMilestoneRequest request) {
        return ApiResponse.<MilestoneResponse>builder()
                .message("Milestone updated successfully")
                .result(milestoneService.updateMilestone(milestoneId, request))
                .build();
    }

    @PatchMapping("/milestones/{milestoneId}/open")
    public ApiResponse<MilestoneResponse> openMilestone(@PathVariable Long milestoneId) {
        return ApiResponse.<MilestoneResponse>builder()
                .message("Milestone opened successfully")
                .result(milestoneService.openMilestone(milestoneId))
                .build();
    }

    @PatchMapping("/milestones/{milestoneId}/close")
    public ApiResponse<MilestoneResponse> closeMilestone(@PathVariable Long milestoneId) {
        return ApiResponse.<MilestoneResponse>builder()
                .message("Milestone closed successfully")
                .result(milestoneService.closeMilestone(milestoneId))
                .build();
    }

    @PatchMapping("/milestones/{milestoneId}/cancel")
    public ApiResponse<MilestoneResponse> cancelMilestone(@PathVariable Long milestoneId) {
        return ApiResponse.<MilestoneResponse>builder()
                .message("Milestone cancelled successfully")
                .result(milestoneService.cancelMilestone(milestoneId))
                .build();
    }

    @DeleteMapping("/milestones/{milestoneId}")
    public ApiResponse<Void> deleteMilestone(@PathVariable Long milestoneId) {
        milestoneService.deleteMilestone(milestoneId);
        return ApiResponse.<Void>builder()
                .message("Milestone deleted successfully")
                .build();
    }
}

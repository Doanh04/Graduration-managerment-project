package com.graduration.Controler.ManagerControler;

import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.graduration.Constain.DefenseScheduleStatusConstain;
import com.graduration.DTO.Request.DefenseScheduleRequest;
import com.graduration.DTO.Request.RescheduleDefenseRequest;
import com.graduration.DTO.Request.ScheduleReasonRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.DefenseScheduleHistoryResponse;
import com.graduration.DTO.Response.DefenseScheduleResponse;
import com.graduration.DTO.Response.DefenseScheduleValidationResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.Service.ManagerService.DefenseScheduleHistoryService;
import com.graduration.Service.ManagerService.DefenseScheduleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class DefenseScheduleControler {
    private final DefenseScheduleService defenseScheduleService;
    private final DefenseScheduleHistoryService defenseScheduleHistoryService;

    @PostMapping("/defense-periods/{periodId}/schedules")
    public ApiResponse<DefenseScheduleResponse> create(
            @PathVariable Long periodId, @Valid @RequestBody DefenseScheduleRequest request) {
        return ApiResponse.<DefenseScheduleResponse>builder()
                .message("Defense schedule created successfully")
                .result(defenseScheduleService.create(periodId, request))
                .build();
    }

    @PostMapping("/defense-periods/{periodId}/schedules/validate")
    public ApiResponse<DefenseScheduleValidationResponse> validate(
            @PathVariable Long periodId, @Valid @RequestBody DefenseScheduleRequest request) {
        return ApiResponse.<DefenseScheduleValidationResponse>builder()
                .result(defenseScheduleService.validate(periodId, request))
                .build();
    }

    @GetMapping("/defense-periods/{periodId}/schedules")
    public ApiResponse<PageResponse<DefenseScheduleResponse>> getByPeriod(
            @PathVariable Long periodId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) Long committeeId,
            @RequestParam(required = false) String room,
            @RequestParam(required = false) DefenseScheduleStatusConstain status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<DefenseScheduleResponse>>builder()
                .result(defenseScheduleService.getByPeriod(periodId, date, committeeId, room, status, page, size))
                .build();
    }

    @GetMapping("/defense-schedules/{scheduleId}")
    public ApiResponse<DefenseScheduleResponse> getById(@PathVariable Long scheduleId) {
        return ApiResponse.<DefenseScheduleResponse>builder()
                .result(defenseScheduleService.getById(scheduleId))
                .build();
    }

    @GetMapping("/defense-schedules/{scheduleId}/history")
    public ApiResponse<PageResponse<DefenseScheduleHistoryResponse>> getHistory(
            @PathVariable Long scheduleId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<DefenseScheduleHistoryResponse>>builder()
                .result(defenseScheduleHistoryService.getHistory(scheduleId, page, size))
                .build();
    }

    @PutMapping("/defense-schedules/{scheduleId}")
    public ApiResponse<DefenseScheduleResponse> update(
            @PathVariable Long scheduleId, @Valid @RequestBody DefenseScheduleRequest request) {
        return ApiResponse.<DefenseScheduleResponse>builder()
                .message("Defense schedule updated successfully")
                .result(defenseScheduleService.update(scheduleId, request))
                .build();
    }

    @PatchMapping("/defense-schedules/{scheduleId}/publish")
    public ApiResponse<DefenseScheduleResponse> publish(@PathVariable Long scheduleId) {
        return ApiResponse.<DefenseScheduleResponse>builder()
                .message("Defense schedule published successfully")
                .result(defenseScheduleService.publish(scheduleId))
                .build();
    }

    @PatchMapping("/defense-schedules/{scheduleId}/postpone")
    public ApiResponse<DefenseScheduleResponse> postpone(
            @PathVariable Long scheduleId, @Valid @RequestBody ScheduleReasonRequest request) {
        return ApiResponse.<DefenseScheduleResponse>builder()
                .message("Defense schedule postponed successfully")
                .result(defenseScheduleService.postpone(scheduleId, request))
                .build();
    }

    @PatchMapping("/defense-schedules/{scheduleId}/reschedule")
    public ApiResponse<DefenseScheduleResponse> reschedule(
            @PathVariable Long scheduleId, @Valid @RequestBody RescheduleDefenseRequest request) {
        return ApiResponse.<DefenseScheduleResponse>builder()
                .message("Defense schedule rescheduled as draft successfully")
                .result(defenseScheduleService.reschedule(scheduleId, request))
                .build();
    }

    @PatchMapping("/defense-schedules/{scheduleId}/complete")
    public ApiResponse<DefenseScheduleResponse> complete(@PathVariable Long scheduleId) {
        return ApiResponse.<DefenseScheduleResponse>builder()
                .message("Defense schedule completed successfully")
                .result(defenseScheduleService.complete(scheduleId))
                .build();
    }

    @PatchMapping("/defense-schedules/{scheduleId}/cancel")
    public ApiResponse<DefenseScheduleResponse> cancel(
            @PathVariable Long scheduleId, @Valid @RequestBody ScheduleReasonRequest request) {
        return ApiResponse.<DefenseScheduleResponse>builder()
                .message("Defense schedule cancelled successfully")
                .result(defenseScheduleService.cancel(scheduleId, request))
                .build();
    }

    @DeleteMapping("/defense-schedules/{scheduleId}")
    public ApiResponse<Void> delete(@PathVariable Long scheduleId) {
        defenseScheduleService.delete(scheduleId);
        return ApiResponse.<Void>builder()
                .message("Defense schedule deleted successfully")
                .build();
    }
}

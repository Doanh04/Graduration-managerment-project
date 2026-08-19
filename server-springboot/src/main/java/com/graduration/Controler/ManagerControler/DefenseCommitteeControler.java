package com.graduration.Controler.ManagerControler;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.graduration.Constain.DefenseCommitteeStatusConstain;
import com.graduration.DTO.Request.DeactivateDefenseCommitteeRequest;
import com.graduration.DTO.Request.DefenseCommitteeRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.DefenseCommitteeResponse;
import com.graduration.DTO.Response.DefenseCommitteeValidationResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.Service.ManagerService.DefenseCommitteeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class DefenseCommitteeControler {
    private final DefenseCommitteeService defenseCommitteeService;

    @PostMapping("/defense-periods/{defensePeriodId}/committees")
    public ApiResponse<DefenseCommitteeResponse> create(
            @PathVariable Long defensePeriodId, @Valid @RequestBody DefenseCommitteeRequest request) {
        return ApiResponse.<DefenseCommitteeResponse>builder()
                .message("Defense committee created successfully")
                .result(defenseCommitteeService.create(defensePeriodId, request))
                .build();
    }

    @GetMapping("/defense-periods/{defensePeriodId}/committees")
    public ApiResponse<PageResponse<DefenseCommitteeResponse>> getByDefensePeriod(
            @PathVariable Long defensePeriodId,
            @RequestParam(required = false) DefenseCommitteeStatusConstain status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<DefenseCommitteeResponse>>builder()
                .result(defenseCommitteeService.getByDefensePeriod(defensePeriodId, status, keyword, page, size))
                .build();
    }

    @GetMapping("/defense-committees/{committeeId}")
    public ApiResponse<DefenseCommitteeResponse> getById(@PathVariable Long committeeId) {
        return ApiResponse.<DefenseCommitteeResponse>builder()
                .result(defenseCommitteeService.getById(committeeId))
                .build();
    }

    @PutMapping("/defense-committees/{committeeId}")
    public ApiResponse<DefenseCommitteeResponse> update(
            @PathVariable Long committeeId, @Valid @RequestBody DefenseCommitteeRequest request) {
        return ApiResponse.<DefenseCommitteeResponse>builder()
                .message("Defense committee updated successfully")
                .result(defenseCommitteeService.update(committeeId, request))
                .build();
    }

    @GetMapping("/defense-committees/{committeeId}/validation")
    public ApiResponse<DefenseCommitteeValidationResponse> validate(@PathVariable Long committeeId) {
        return ApiResponse.<DefenseCommitteeValidationResponse>builder()
                .result(defenseCommitteeService.validate(committeeId))
                .build();
    }

    @PatchMapping("/defense-committees/{committeeId}/activate")
    public ApiResponse<DefenseCommitteeResponse> activate(@PathVariable Long committeeId) {
        return ApiResponse.<DefenseCommitteeResponse>builder()
                .message("Defense committee activated successfully")
                .result(defenseCommitteeService.activate(committeeId))
                .build();
    }

    @PatchMapping("/defense-committees/{committeeId}/draft")
    public ApiResponse<DefenseCommitteeResponse> moveToDraft(@PathVariable Long committeeId) {
        return ApiResponse.<DefenseCommitteeResponse>builder()
                .message("Defense committee moved to draft successfully")
                .result(defenseCommitteeService.moveToDraft(committeeId))
                .build();
    }

    @PatchMapping("/defense-committees/{committeeId}/deactivate")
    public ApiResponse<DefenseCommitteeResponse> deactivate(
            @PathVariable Long committeeId, @Valid @RequestBody DeactivateDefenseCommitteeRequest request) {
        return ApiResponse.<DefenseCommitteeResponse>builder()
                .message("Defense committee deactivated successfully")
                .result(defenseCommitteeService.deactivate(committeeId, request))
                .build();
    }

    @DeleteMapping("/defense-committees/{committeeId}")
    public ApiResponse<Void> delete(@PathVariable Long committeeId) {
        defenseCommitteeService.delete(committeeId);
        return ApiResponse.<Void>builder()
                .message("Defense committee deleted successfully")
                .build();
    }
}

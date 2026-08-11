package com.graduration.Controler.AcademicYear;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.graduration.DTO.Request.DefensePeriodRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.DefensePeriodResponse;
import com.graduration.Service.AcademicService.DefensePeriodService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/defense-period")
@RequiredArgsConstructor
public class DefensePeriodControler {
    private final DefensePeriodService defensePeriodService;

    @PostMapping("/create-defense-period")
    public ApiResponse<DefensePeriodResponse> createDefensePeriod(@Valid @RequestBody DefensePeriodRequest request) {
        return ApiResponse.<DefensePeriodResponse>builder()
                .message("Defense period created successfully")
                .result(defensePeriodService.createDefensePeriod(request))
                .build();
    }

    @GetMapping("/{defensePeriodId}")
    public ApiResponse<DefensePeriodResponse> getDefensePeriod(@PathVariable Long defensePeriodId) {
        return ApiResponse.<DefensePeriodResponse>builder()
                .result(defensePeriodService.getDefensePeriod(defensePeriodId))
                .build();
    }

    @GetMapping("/get-all-defense-period")
    public ApiResponse<List<DefensePeriodResponse>> getAllDefensePeriods(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer size) {
        return ApiResponse.<List<DefensePeriodResponse>>builder()
                .result(
                        page == null && size == null
                                ? defensePeriodService.getAllDefensePeriods()
                                : defensePeriodService.getAllDefensePeriods(page, size))
                .build();
    }

    @GetMapping("/academic-year/{academicId}")
    public ApiResponse<List<DefensePeriodResponse>> getDefensePeriodsByAcademicYear(
            @PathVariable Integer academicId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer size) {
        return ApiResponse.<List<DefensePeriodResponse>>builder()
                .result(
                        page == null && size == null
                                ? defensePeriodService.getDefensePeriodsByAcademicYear(academicId)
                                : defensePeriodService.getDefensePeriodsByAcademicYear(academicId, page, size))
                .build();
    }

    @PutMapping("/{defensePeriodId}")
    public ApiResponse<DefensePeriodResponse> updateDefensePeriod(
            @PathVariable Long defensePeriodId, @Valid @RequestBody DefensePeriodRequest request) {
        return ApiResponse.<DefensePeriodResponse>builder()
                .message("Defense period updated successfully")
                .result(defensePeriodService.updateDefensePeriod(defensePeriodId, request))
                .build();
    }

    @DeleteMapping("/{defensePeriodId}")
    public ApiResponse<Void> deleteDefensePeriod(@PathVariable Long defensePeriodId) {
        defensePeriodService.deleteDefensePeriod(defensePeriodId);
        return ApiResponse.<Void>builder()
                .message("Defense period deleted successfully")
                .build();
    }
}

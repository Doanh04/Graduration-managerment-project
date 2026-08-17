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

import com.graduration.DTO.Request.ScoreCriterionRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.ScoreCriterionResponse;
import com.graduration.Service.GradurationService.ScoreCriterionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ScoreCriterionControler {
    private final ScoreCriterionService scoreCriterionService;

    @PostMapping("/defense-periods/{defensePeriodId}/score-criteria")
    public ApiResponse<ScoreCriterionResponse> create(
            @PathVariable Long defensePeriodId, @Valid @RequestBody ScoreCriterionRequest request) {
        return ApiResponse.<ScoreCriterionResponse>builder()
                .message("Score criterion created successfully")
                .result(scoreCriterionService.create(defensePeriodId, request))
                .build();
    }

    @GetMapping("/defense-periods/{defensePeriodId}/score-criteria")
    public ApiResponse<PageResponse<ScoreCriterionResponse>> getByDefensePeriod(
            @PathVariable Long defensePeriodId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<ScoreCriterionResponse>>builder()
                .result(scoreCriterionService.getByDefensePeriod(defensePeriodId, page, size))
                .build();
    }

    @PutMapping("/score-criteria/{criterionId}")
    public ApiResponse<ScoreCriterionResponse> update(
            @PathVariable Long criterionId, @Valid @RequestBody ScoreCriterionRequest request) {
        return ApiResponse.<ScoreCriterionResponse>builder()
                .message("Score criterion updated successfully")
                .result(scoreCriterionService.update(criterionId, request))
                .build();
    }

    @DeleteMapping("/score-criteria/{criterionId}")
    public ApiResponse<Void> delete(@PathVariable Long criterionId) {
        scoreCriterionService.delete(criterionId);
        return ApiResponse.<Void>builder()
                .message("Score criterion deleted successfully")
                .build();
    }
}

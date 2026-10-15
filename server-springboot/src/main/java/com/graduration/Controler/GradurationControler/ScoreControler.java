package com.graduration.Controler.GradurationControler;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.graduration.DTO.Request.ScoreRequest;
import com.graduration.DTO.Request.CommitteeScoreSheetRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.ScoreResponse;
import com.graduration.DTO.Response.CommitteeScoreSheetResponse;
import com.graduration.Service.GradurationService.ScoreService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ScoreControler {
    private final ScoreService scoreService;

    @PutMapping("/students/{studentId}/topics/{topicId}/score")
    public ApiResponse<ScoreResponse> saveDraft(
            @PathVariable String studentId, @PathVariable Long topicId, @Valid @RequestBody ScoreRequest request) {
        return ApiResponse.<ScoreResponse>builder()
                .message("Score draft saved successfully")
                .result(scoreService.saveDraft(studentId, topicId, request))
                .build();
    }

    @GetMapping("/scores/{scoreId}")
    public ApiResponse<ScoreResponse> getScore(@PathVariable Long scoreId) {
        return ApiResponse.<ScoreResponse>builder()
                .result(scoreService.getScore(scoreId))
                .build();
    }

    @GetMapping("/students/{studentId}/topics/{topicId}/score")
    public ApiResponse<ScoreResponse> getCurrentScore(@PathVariable String studentId, @PathVariable Long topicId) {
        return ApiResponse.<ScoreResponse>builder()
                .result(scoreService.getCurrentScore(studentId, topicId))
                .build();
    }

    @GetMapping("/students/{studentId}/topics/{topicId}/committee-score-sheet")
    public ApiResponse<CommitteeScoreSheetResponse> getCommitteeScoreSheet(
            @PathVariable String studentId, @PathVariable Long topicId) {
        return ApiResponse.<CommitteeScoreSheetResponse>builder()
                .result(scoreService.getCommitteeScoreSheet(studentId, topicId))
                .build();
    }

    @PutMapping("/students/{studentId}/topics/{topicId}/committee-score-sheet")
    public ApiResponse<CommitteeScoreSheetResponse> saveCommitteeScoreSheet(
            @PathVariable String studentId,
            @PathVariable Long topicId,
            @Valid @RequestBody CommitteeScoreSheetRequest request) {
        return ApiResponse.<CommitteeScoreSheetResponse>builder()
                .message("Committee score sheet saved successfully")
                .result(scoreService.saveCommitteeScoreSheet(studentId, topicId, request))
                .build();
    }

    @PatchMapping("/students/{studentId}/topics/{topicId}/committee-score-sheet/submit")
    public ApiResponse<CommitteeScoreSheetResponse> submitCommitteeScoreSheet(
            @PathVariable String studentId, @PathVariable Long topicId) {
        return ApiResponse.<CommitteeScoreSheetResponse>builder()
                .message("Committee score sheet submitted successfully")
                .result(scoreService.submitCommitteeScoreSheet(studentId, topicId))
                .build();
    }

    @PatchMapping("/scores/{scoreId}/submit")
    public ApiResponse<ScoreResponse> submit(@PathVariable Long scoreId) {
        return ApiResponse.<ScoreResponse>builder()
                .message("Score submitted and locked pending final submission approval")
                .result(scoreService.submit(scoreId))
                .build();
    }

    @PatchMapping("/scores/{scoreId}/publish")
    public ApiResponse<ScoreResponse> publish(@PathVariable Long scoreId) {
        return ApiResponse.<ScoreResponse>builder()
                .message("Score published successfully")
                .result(scoreService.publish(scoreId))
                .build();
    }

    @PatchMapping("/scores/{scoreId}/unlock")
    public ApiResponse<ScoreResponse> unlock(@PathVariable Long scoreId) {
        return ApiResponse.<ScoreResponse>builder()
                .message("Score unlocked successfully")
                .result(scoreService.unlock(scoreId))
                .build();
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/scores/{scoreId}")
    public ApiResponse<Void> delete(@PathVariable Long scoreId) {
        scoreService.delete(scoreId);
        return ApiResponse.<Void>builder().message("Score deleted successfully").build();
    }

    @GetMapping("/defense-periods/{defensePeriodId}/scores")
    public ApiResponse<PageResponse<ScoreResponse>> getByDefensePeriod(
            @PathVariable Long defensePeriodId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<ScoreResponse>>builder()
                .result(scoreService.getByDefensePeriod(defensePeriodId, page, size))
                .build();
    }

    @GetMapping("/scores/my")
    public ApiResponse<PageResponse<ScoreResponse>> getMyPublishedScores() {
        return ApiResponse.<PageResponse<ScoreResponse>>builder()
                .result(scoreService.getMyPublishedScores())
                .build();
    }
}

package com.graduration.Controler.ManagerControler;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.graduration.DTO.Request.AssignCommitteeMemberRequest;
import com.graduration.DTO.Request.DeactivateCommitteeMemberRequest;
import com.graduration.DTO.Request.UpdateCommitteeMemberRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.CommitteeMemberResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.Service.ManagerService.CommitteeMemberService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CommitteeMemberControler {
    private final CommitteeMemberService committeeMemberService;

    @PostMapping("/defense-committees/{committeeId}/members")
    public ApiResponse<CommitteeMemberResponse> assign(
            @PathVariable Long committeeId, @Valid @RequestBody AssignCommitteeMemberRequest request) {
        return ApiResponse.<CommitteeMemberResponse>builder()
                .message("Committee member assigned successfully")
                .result(committeeMemberService.assign(committeeId, request))
                .build();
    }

    @GetMapping("/defense-committees/{committeeId}/members")
    public ApiResponse<PageResponse<CommitteeMemberResponse>> getByCommittee(
            @PathVariable Long committeeId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<CommitteeMemberResponse>>builder()
                .result(committeeMemberService.getByCommittee(committeeId, page, size))
                .build();
    }

    @GetMapping("/lecturers/{lectureId}/committee-assignments")
    public ApiResponse<PageResponse<CommitteeMemberResponse>> getByLecturer(
            @PathVariable String lectureId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<CommitteeMemberResponse>>builder()
                .result(committeeMemberService.getByLecturer(lectureId, page, size))
                .build();
    }

    @GetMapping("/committee-members/me")
    public ApiResponse<PageResponse<CommitteeMemberResponse>> getMine(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<CommitteeMemberResponse>>builder()
                .result(committeeMemberService.getMine(page, size))
                .build();
    }

    @PatchMapping("/committee-members/{memberId}")
    public ApiResponse<CommitteeMemberResponse> update(
            @PathVariable Long memberId, @Valid @RequestBody UpdateCommitteeMemberRequest request) {
        return ApiResponse.<CommitteeMemberResponse>builder()
                .message("Committee member assignment updated successfully")
                .result(committeeMemberService.update(memberId, request))
                .build();
    }

    @PatchMapping("/committee-members/{memberId}/deactivate")
    public ApiResponse<CommitteeMemberResponse> deactivate(
            @PathVariable Long memberId, @Valid @RequestBody DeactivateCommitteeMemberRequest request) {
        return ApiResponse.<CommitteeMemberResponse>builder()
                .message("Committee member assignment deactivated successfully")
                .result(committeeMemberService.deactivate(memberId, request))
                .build();
    }
}

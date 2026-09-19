package com.graduration.Controler.GradurationControler;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.graduration.Constain.EnrollmentStatusConstain;
import com.graduration.DTO.Request.BulkCreateGraduationEnrollmentRequest;
import com.graduration.DTO.Request.CreateGraduationEnrollmentRequest;
import com.graduration.DTO.Request.UpdateGraduationEnrollmentRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.GraduationEnrollmentResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.Service.GradurationService.GraduationEnrollmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/graduation-enrollments")
@RequiredArgsConstructor
public class GraduationEnrollmentControler {
    private final GraduationEnrollmentService service;

    @PostMapping
    public ApiResponse<GraduationEnrollmentResponse> create(
            @Valid @RequestBody CreateGraduationEnrollmentRequest request) {
        return ApiResponse.<GraduationEnrollmentResponse>builder()
                .message("Ghi danh sinh viên thành công")
                .result(service.create(request))
                .build();
    }

    @PostMapping("/bulk")
    public ApiResponse<List<GraduationEnrollmentResponse>> createBulk(
            @Valid @RequestBody BulkCreateGraduationEnrollmentRequest request) {
        return ApiResponse.<List<GraduationEnrollmentResponse>>builder()
                .message("Ghi danh danh sách sinh viên thành công")
                .result(service.createBulk(request))
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<GraduationEnrollmentResponse>> getAll(
            @RequestParam(required = false) Long defensePeriodId,
            @RequestParam(required = false) EnrollmentStatusConstain status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.<PageResponse<GraduationEnrollmentResponse>>builder()
                .result(service.getAll(defensePeriodId, status, keyword, page, size))
                .build();
    }

    @GetMapping("/my")
    public ApiResponse<List<GraduationEnrollmentResponse>> mine() {
        return ApiResponse.<List<GraduationEnrollmentResponse>>builder()
                .result(service.getMine())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<GraduationEnrollmentResponse> get(@PathVariable Long id) {
        return ApiResponse.<GraduationEnrollmentResponse>builder()
                .result(service.get(id))
                .build();
    }

    @PatchMapping("/{id}")
    public ApiResponse<GraduationEnrollmentResponse> update(
            @PathVariable Long id, @Valid @RequestBody UpdateGraduationEnrollmentRequest request) {
        return ApiResponse.<GraduationEnrollmentResponse>builder()
                .message("Cập nhật ghi danh thành công")
                .result(service.update(id, request))
                .build();
    }

    @PatchMapping("/{id}/withdraw")
    public ApiResponse<GraduationEnrollmentResponse> withdraw(@PathVariable Long id) {
        return ApiResponse.<GraduationEnrollmentResponse>builder()
                .message("Đã hủy tư cách thi của sinh viên")
                .result(service.withdraw(id))
                .build();
    }
}

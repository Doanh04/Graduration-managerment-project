package com.graduration.Controler.AcademicYear;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.graduration.DTO.Request.AcademicYearRequest;
import com.graduration.DTO.Response.AcademicYearResponse;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.Service.AcademicService.AcademicYearService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/academic-year")
@RequiredArgsConstructor
public class AcademicYearControler {
    private final AcademicYearService academicYearService;

    @PostMapping("/create-academic-year")
    public ApiResponse<AcademicYearResponse> createAcademicYear(@Valid @RequestBody AcademicYearRequest request) {
        return ApiResponse.<AcademicYearResponse>builder()
                .message("Academic year created successfully")
                .result(academicYearService.createAcademicYear(request))
                .build();
    }

    @GetMapping("/{academicId}")
    public ApiResponse<AcademicYearResponse> getAcademicYear(@PathVariable Integer academicId) {
        return ApiResponse.<AcademicYearResponse>builder()
                .result(academicYearService.getAcademicYear(academicId))
                .build();
    }

    @GetMapping("/name/{academicYear}")
    public ApiResponse<AcademicYearResponse> getAcademicYearByName(@PathVariable String academicYear) {
        return ApiResponse.<AcademicYearResponse>builder()
                .result(academicYearService.getAcademicYearByName(academicYear))
                .build();
    }

    @GetMapping("/get-all-academic-year")
    public ApiResponse<com.graduration.DTO.Response.PageResponse<AcademicYearResponse>> getAllAcademicYears(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer size) {
        return ApiResponse.<com.graduration.DTO.Response.PageResponse<AcademicYearResponse>>builder()
                .result(academicYearService.getAllAcademicYearsPage(page, size))
                .build();
    }

    @PutMapping("/{academicId}")
    public ApiResponse<AcademicYearResponse> updateAcademicYear(
            @PathVariable Integer academicId, @Valid @RequestBody AcademicYearRequest request) {
        return ApiResponse.<AcademicYearResponse>builder()
                .message("Academic year updated successfully")
                .result(academicYearService.updateAcademicYear(academicId, request))
                .build();
    }

    @DeleteMapping("/{academicId}")
    public ApiResponse<Void> deleteAcademicYear(@PathVariable Integer academicId) {
        academicYearService.deleteAcademicYear(academicId);
        return ApiResponse.<Void>builder()
                .message("Academic year deleted successfully")
                .build();
    }
}

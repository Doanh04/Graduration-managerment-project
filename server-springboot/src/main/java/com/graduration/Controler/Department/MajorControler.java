package com.graduration.Controler.Department;

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

import com.graduration.DTO.Request.MajorRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.MajorResponse;
import com.graduration.Service.DerpatmentService.MajorService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/major")
@RequiredArgsConstructor
public class MajorControler {
    private final MajorService majorService;

    @PostMapping("/create-major")
    public ApiResponse<MajorResponse> createMajor(@Valid @RequestBody MajorRequest request) {
        return ApiResponse.<MajorResponse>builder()
                .message("Major created successfully")
                .result(majorService.createMajor(request))
                .build();
    }

    @GetMapping("/{majorId}")
    public ApiResponse<MajorResponse> getMajor(@PathVariable Long majorId) {
        return ApiResponse.<MajorResponse>builder()
                .result(majorService.getMajor(majorId))
                .build();
    }

    @GetMapping("/get-all-major")
    public ApiResponse<List<MajorResponse>> getAllMajors() {
        return ApiResponse.<List<MajorResponse>>builder()
                .result(majorService.getAllMajors())
                .build();
    }

    @PutMapping("/{majorId}")
    public ApiResponse<MajorResponse> updateMajor(
            @PathVariable Long majorId, @Valid @RequestBody MajorRequest request) {
        return ApiResponse.<MajorResponse>builder()
                .message("Major updated successfully")
                .result(majorService.updateMajor(majorId, request))
                .build();
    }

    @DeleteMapping("/{majorId}")
    public ApiResponse<Void> deleteMajor(@PathVariable Long majorId) {
        majorService.deleteMajor(majorId);
        return ApiResponse.<Void>builder().message("Major deleted successfully").build();
    }
}

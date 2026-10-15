package com.graduration.Controler;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.graduration.DTO.Request.UpdateSystemSettingRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.SystemSettingResponse;
import com.graduration.Service.SystemSettingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/system-settings")
@RequiredArgsConstructor
public class SystemSettingControler {
    private final SystemSettingService systemSettingService;

    @GetMapping
    public ApiResponse<SystemSettingResponse> getSettings() {
        return ApiResponse.<SystemSettingResponse>builder()
                .result(systemSettingService.getSettings())
                .build();
    }

    @PutMapping
    public ApiResponse<SystemSettingResponse> updateSettings(@Valid @RequestBody UpdateSystemSettingRequest request) {
        return ApiResponse.<SystemSettingResponse>builder()
                .message("Cập nhật cài đặt hệ thống thành công")
                .result(systemSettingService.updateSettings(request))
                .build();
    }
}

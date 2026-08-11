package com.graduration.Controler.UserControler;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.graduration.Constain.PermissionConstain;
import com.graduration.DTO.Request.CreatePermissionRequest;
import com.graduration.DTO.Request.UpdatePermissionRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.PermissionResponse;
import com.graduration.Service.UserService.PermissionService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/permission")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionControler {
    PermissionService permissionService;

    @PostMapping({"", "/create-permission"})
    public ApiResponse<PermissionResponse> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        return ApiResponse.<PermissionResponse>builder()
                .message("Permission created successfully")
                .result(permissionService.createPermission(request))
                .build();
    }

    @PutMapping("/{permissionId}")
    public ApiResponse<PermissionResponse> updatePermission(
            @PathVariable PermissionConstain permissionId, @Valid @RequestBody UpdatePermissionRequest request) {
        return ApiResponse.<PermissionResponse>builder()
                .message("Permission updated successfully")
                .result(permissionService.updatePermission(permissionId, request))
                .build();
    }

    @DeleteMapping("/{permissionId}")
    public ApiResponse<Void> deletePermission(@PathVariable PermissionConstain permissionId) {
        permissionService.deletePermission(permissionId);
        return ApiResponse.<Void>builder()
                .message("Permission deleted successfully")
                .build();
    }

    @GetMapping("/{permissionId}")
    public ApiResponse<PermissionResponse> getPermission(@PathVariable PermissionConstain permissionId) {
        return ApiResponse.<PermissionResponse>builder()
                .result(permissionService.getPermission(permissionId))
                .build();
    }

    @GetMapping({"", "/get-permission"})
    public ApiResponse<List<PermissionResponse>> getAllPermissions(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ApiResponse.<List<PermissionResponse>>builder()
                .result(
                        page == null && size == null
                                ? permissionService.getAllPermissions()
                                : permissionService.getAllPermissions(page, size))
                .build();
    }
}

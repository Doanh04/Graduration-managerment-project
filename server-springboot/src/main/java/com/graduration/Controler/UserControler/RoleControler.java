package com.graduration.Controler.UserControler;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.graduration.Constain.RoleConstain;
import com.graduration.DTO.Request.CreateRoleRequest;
import com.graduration.DTO.Request.UpdateRoleRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.RoleResponse;
import com.graduration.Service.RoleService.RoleService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleControler {
    RoleService roleService;

    @PostMapping({"", "/create-role"})
    public ApiResponse<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ApiResponse.<RoleResponse>builder()
                .message("Role created successfully")
                .result(roleService.createRole(request))
                .build();
    }

    @PutMapping("/{roleId}")
    public ApiResponse<RoleResponse> updateRole(
            @PathVariable RoleConstain roleId, @Valid @RequestBody UpdateRoleRequest request) {
        return ApiResponse.<RoleResponse>builder()
                .message("Role updated successfully")
                .result(roleService.updateRole(roleId, request))
                .build();
    }

    @DeleteMapping("/{roleId}")
    public ApiResponse<Void> deleteRole(@PathVariable RoleConstain roleId) {
        roleService.deleteRole(roleId);
        return ApiResponse.<Void>builder().message("Role deleted successfully").build();
    }

    @GetMapping("/{roleId}")
    public ApiResponse<RoleResponse> getRole(@PathVariable RoleConstain roleId) {
        return ApiResponse.<RoleResponse>builder()
                .result(roleService.getRole(roleId))
                .build();
    }

    @GetMapping
    public ApiResponse<List<RoleResponse>> getAllRoles(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return ApiResponse.<List<RoleResponse>>builder()
                .result(page == null && size == null ? roleService.getAllRoles() : roleService.getAllRoles(page, size))
                .build();
    }
}

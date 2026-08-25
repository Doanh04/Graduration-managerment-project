package com.graduration.Controler.UserControler;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.graduration.DTO.Request.UpdateUserRolesRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.UserRoleResponse;
import com.graduration.Service.UserService.UserRoleService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/user-role")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserRoleControler {
    UserRoleService userRoleService;

    @GetMapping
    public ApiResponse<List<UserRoleResponse>> getUsers() {
        return ApiResponse.<List<UserRoleResponse>>builder()
                .result(userRoleService.getUsers())
                .build();
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserRoleResponse> updateRoles(
            @PathVariable String userId, @RequestBody UpdateUserRolesRequest request) {
        return ApiResponse.<UserRoleResponse>builder()
                .message("User roles updated successfully")
                .result(userRoleService.updateRoles(userId, request))
                .build();
    }
}

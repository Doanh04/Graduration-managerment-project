package com.graduration.Service.UserService;

import java.util.HashSet;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Constain.RoleConstain;
import com.graduration.DTO.Request.UpdateUserRolesRequest;
import com.graduration.DTO.Response.UserRoleResponse;
import com.graduration.Repository.RoleRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.Roles;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserRoleService {
    UserRepository userRepository;
    RoleRepository roleRepository;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public List<UserRoleResponse> getUsers() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    public UserRoleResponse updateRoles(String userId, UpdateUserRolesRequest request) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        var roleIds = request == null || request.getRoles() == null ? new HashSet<RoleConstain>() : request.getRoles();
        List<Roles> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        }
        user.setRoles(new HashSet<>(roles));
        return toResponse(userRepository.save(user));
    }

    private UserRoleResponse toResponse(UserEntity user) {
        String fullName = user.getStudent() != null
                ? user.getStudent().getFullNameStudent()
                : user.getLecture() != null ? user.getLecture().getFullNameLecture() : user.getUserName();
        String userCode = user.getStudent() != null
                ? user.getStudent().getStudentCode()
                : user.getLecture() != null ? user.getLecture().getLectureCode() : null;
        String userType = user.getStudent() != null ? "STUDENT" : user.getLecture() != null ? "LECTURER" : "SYSTEM";
        return UserRoleResponse.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .fullName(fullName)
                .userCode(userCode)
                .userType(userType)
                .status(user.getStatus())
                .roles(user.getRoles().stream().map(Roles::getRole).collect(java.util.stream.Collectors.toSet()))
                .build();
    }
}

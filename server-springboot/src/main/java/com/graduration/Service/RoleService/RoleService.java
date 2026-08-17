package com.graduration.Service.RoleService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.PermissionConstain;
import com.graduration.Constain.RoleConstain;
import com.graduration.DTO.Request.CreateRoleRequest;
import com.graduration.DTO.Request.UpdateRoleRequest;
import com.graduration.DTO.Response.RoleResponse;
import com.graduration.Repository.PermissionRepository;
import com.graduration.Repository.RoleRepository;
import com.graduration.entity.PermissionEntity;
import com.graduration.entity.Roles;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.RoleMaper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleService {
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    RoleMaper roleMaper;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.existsById(request.getRole()) || roleRepository.existsByRoleName(request.getRoleName())) {
            throw new AppException(ErrorCode.ROLE_IS_EXITED);
        }

        Roles role = roleMaper.toRoleEntity(request);
        role.setPermission(findPermissions(request.getPermissions()));

        return roleMaper.toRoleResponse(roleRepository.save(role));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    public RoleResponse updateRole(RoleConstain roleId, UpdateRoleRequest request) {
        Roles role = findRole(roleId);

        if (roleRepository.existsByRoleNameAndRoleNot(request.getRoleName(), roleId)) {
            throw new AppException(ErrorCode.ROLE_IS_EXITED);
        }

        roleMaper.updateRoleEntity(request, role);
        role.setPermission(findPermissions(request.getPermissions()));

        return roleMaper.toRoleResponse(roleRepository.save(role));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    public void deleteRole(RoleConstain roleId) {
        Roles role = findRole(roleId);

        role.getUser().forEach(user -> user.getRoles().remove(role));
        role.getUser().clear();
        role.getPermission().clear();
        roleRepository.delete(role);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public RoleResponse getRole(RoleConstain roleId) {
        return roleMaper.toRoleResponse(findRole(roleId));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return getAllRoles(0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles(Integer page, Integer size) {
        return roleRepository.findAll(PaginationSupport.pageRequest(page, size)).stream()
                .map(roleMaper::toRoleResponse)
                .toList();
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public com.graduration.DTO.Response.PageResponse<RoleResponse> getAllRolesPage(Integer page, Integer size) {
        return com.graduration.DTO.Response.PageResponse.from(
                roleRepository.findAll(PaginationSupport.pageRequest(page, size)), roleMaper::toRoleResponse);
    }

    private Roles findRole(RoleConstain roleId) {
        return roleRepository.findById(roleId).orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
    }

    private Set<PermissionEntity> findPermissions(Set<PermissionConstain> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }

        List<PermissionEntity> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new AppException(ErrorCode.PERMISSION_NOT_FOUND);
        }

        return new HashSet<>(permissions);
    }
}

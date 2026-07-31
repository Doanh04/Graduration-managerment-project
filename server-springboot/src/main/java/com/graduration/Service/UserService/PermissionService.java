package com.graduration.Service.UserService;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Constain.PermissionConstain;
import com.graduration.DTO.Request.CreatePermissionRequest;
import com.graduration.DTO.Request.UpdatePermissionRequest;
import com.graduration.DTO.Response.PermissionResponse;
import com.graduration.Repository.PermissionRepository;
import com.graduration.entity.PermissionEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.PermissioMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class
PermissionService {
    PermissionRepository permissionRepository;
    PermissioMapper permissioMapper;

    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        if (permissionRepository.existsById(request.getPermissionId())
                || permissionRepository.existsByPermissionName(request.getPermissionName())) {
            throw new AppException(ErrorCode.PERMISSION_IS_EXITED);
        }

        PermissionEntity permission = permissioMapper.toPermissionEntity(request);
        return permissioMapper.toPermissionResponse(permissionRepository.save(permission));
    }

    @Transactional
    public PermissionResponse updatePermission(
            PermissionConstain permissionId, UpdatePermissionRequest request) {
        PermissionEntity permission = findPermission(permissionId);

        if (permissionRepository.existsByPermissionNameAndPermissionIdNot(
                request.getPermissionName(), permissionId)) {
            throw new AppException(ErrorCode.PERMISSION_IS_EXITED);
        }

        permissioMapper.updatePermissionEntity(request, permission);
        return permissioMapper.toPermissionResponse(permissionRepository.save(permission));
    }

    @Transactional
    public void deletePermission(PermissionConstain permissionId) {
        PermissionEntity permission = findPermission(permissionId);

        permission.getRoles().forEach(role -> role.getPermission().remove(permission));
        permission.getRoles().clear();
        permissionRepository.delete(permission);
    }

    @Transactional(readOnly = true)
    public PermissionResponse getPermission(PermissionConstain permissionId) {
        return permissioMapper.toPermissionResponse(findPermission(permissionId));
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(permissioMapper::toPermissionResponse)
                .toList();
    }

    private PermissionEntity findPermission(PermissionConstain permissionId) {
        return permissionRepository
                .findById(permissionId)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));
    }
}

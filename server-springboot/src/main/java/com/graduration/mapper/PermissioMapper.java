package com.graduration.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.graduration.Constain.PermissionConstain;
import com.graduration.DTO.Request.CreatePermissionRequest;
import com.graduration.DTO.Request.UpdatePermissionRequest;
import com.graduration.DTO.Response.PermissionResponse;
import com.graduration.entity.PermissionEntity;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PermissioMapper {
    @Mapping(target = "roles", ignore = true)
    PermissionEntity toPermissionEntity(CreatePermissionRequest request);

    @Mapping(target = "permissionId", ignore = true)
    @Mapping(target = "roles", ignore = true)
    void updatePermissionEntity(UpdatePermissionRequest request, @MappingTarget PermissionEntity permission);

    PermissionResponse toPermissionResponse(PermissionEntity permission);

    default PermissionConstain toPermissionId(PermissionEntity permission) {
        return permission == null ? null : permission.getPermissionId();
    }

    default PermissionEntity toPermissionEntity(PermissionConstain permissionId) {
        if (permissionId == null) {
            return null;
        }

        PermissionEntity permission = new PermissionEntity();
        permission.setPermissionId(permissionId);
        return permission;
    }
}

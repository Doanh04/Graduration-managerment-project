package com.graduration.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.graduration.Constain.RoleConstain;
import com.graduration.DTO.Request.CreateRoleRequest;
import com.graduration.DTO.Request.UpdateRoleRequest;
import com.graduration.DTO.Response.RoleResponse;
import com.graduration.entity.Roles;

@Mapper(componentModel = "spring", uses = PermissioMapper.class, builder = @Builder(disableBuilder = true))
public interface RoleMaper {
    default RoleConstain toRoleId(Roles role) {
        return role == null ? null : role.getRole();
    }

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "permission", ignore = true)
    Roles toRoleEntity(CreateRoleRequest request);

    @Mapping(target = "role", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "permission", ignore = true)
    void updateRoleEntity(UpdateRoleRequest request, @MappingTarget Roles role);

    @Mapping(source = "permission", target = "permissions")
    RoleResponse toRoleResponse(Roles role);
}

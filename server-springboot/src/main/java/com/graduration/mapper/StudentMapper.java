package com.graduration.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.graduration.Constain.PermissionConstain;
import com.graduration.Constain.RoleConstain;
import com.graduration.DTO.Request.RegisterStudentRequest;
import com.graduration.DTO.Response.RegisterStudentResponse;
import com.graduration.entity.Roles;
import com.graduration.entity.StudentEntity;
import com.graduration.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface StudentMapper {
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "avt", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "lecture", ignore = true)
    UserEntity toUserEntity(RegisterStudentRequest request);

    @Mapping(source = "fullName", target = "fullNameStudent")
    @Mapping(source = "phone", target = "phoneStudent")
    @Mapping(source = "classId", target = "classEntity.classId")
    @Mapping(target = "idStudent", ignore = true)
    @Mapping(target = "pathAvt", ignore = true)
    @Mapping(target = "userEntity", ignore = true)
    @Mapping(target = "team", ignore = true)
    StudentEntity toStudentEntity(RegisterStudentRequest request);

    @Mapping(source = "user.userId", target = "idUser")
    @Mapping(source = "user.userName", target = "userName")
    @Mapping(source = "user.status", target = "status")
    @Mapping(source = "user.createAt", target = "createAt")
    @Mapping(source = "student.studentCode", target = "studentCode")
    @Mapping(source = "student.fullNameStudent", target = "fullName")
    @Mapping(source = "student.phoneStudent", target = "phone")
    @Mapping(source = "student.email", target = "email")
    @Mapping(source = "user.roles", target = "roles")
    @Mapping(source = "user.roles", target = "permissions")
    RegisterStudentResponse toStudentResponse(UserEntity user, StudentEntity student);

    default LocalDate toLocalDate(LocalDateTime value) {
        return value == null ? null : value.toLocalDate();
    }

    default Set<RoleConstain> toRoleIds(Set<Roles> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptySet();
        }

        return roles.stream().map(Roles::getRole).collect(Collectors.toSet());
    }

    default Set<PermissionConstain> toPermissionIds(Set<Roles> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptySet();
        }

        return roles.stream()
                .filter(role -> role.getPermission() != null)
                .flatMap(role -> role.getPermission().stream())
                .map(permission -> permission.getPermissionId())
                .collect(Collectors.toSet());
    }
}

package com.graduration.mapper;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.graduration.Constain.PermissionConstain;
import com.graduration.DTO.Request.RegisterLectureRequest;
import com.graduration.DTO.Request.UpdateLecturerRequest;
import com.graduration.DTO.Response.RegisterLectureResponse;
import com.graduration.entity.LectureEntity;
import com.graduration.entity.Roles;
import com.graduration.entity.UserEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = RoleMaper.class)
public interface UserMaper {
    @Mapping(source = "userName", target = "userName")
    @Mapping(source = "password", target = "password")
    UserEntity toUserEntity(RegisterLectureRequest registerLectureRequest);

    @Mapping(source = "lectureCode", target = "lectureCode")
    @Mapping(source = "fullName", target = "fullNameLecture")
    @Mapping(source = "degree", target = "degree")
    @Mapping(source = "email", target = "emaillecture")
    @Mapping(source = "phone", target = "phoneLecture")
    LectureEntity toLecturerEntity(RegisterLectureRequest registerLectureRequest2);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "avt", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "student", ignore = true)
    @Mapping(target = "lecture", ignore = true)
    void updateUserEntity(UpdateLecturerRequest request, @MappingTarget UserEntity user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "fullName", target = "fullNameLecture")
    @Mapping(source = "email", target = "emaillecture")
    @Mapping(source = "phone", target = "phoneLecture")
    @Mapping(target = "lectureId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "comment", ignore = true)
    @Mapping(target = "topicSuperVisor", ignore = true)
    @Mapping(target = "score", ignore = true)
    @Mapping(target = "reviewAssignment", ignore = true)
    @Mapping(target = "comitteesMember", ignore = true)
    void updateLecturerEntity(UpdateLecturerRequest request, @MappingTarget LectureEntity lecturer);

    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "user.userName", target = "userName")
    @Mapping(source = "lecturer.lectureCode", target = "lecturerCode")
    @Mapping(source = "lecturer.fullNameLecture", target = "fullName")
    @Mapping(source = "lecturer.degree", target = "degree")
    @Mapping(source = "lecturer.emaillecture", target = "email")
    @Mapping(source = "lecturer.phoneLecture", target = "phone")
    @Mapping(source = "user.createAt", target = "createAt")
    @Mapping(source = "user.status", target = "status")
    @Mapping(source = "user.roles", target = "roles")
    @Mapping(source = "user.roles", target = "permissions")
    RegisterLectureResponse toLectureResponse(UserEntity user, LectureEntity lecturer);

    default Set<PermissionConstain> toPermissionIds(Set<Roles> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptySet();
        }

        return roles.stream()
                .flatMap(role -> role.getPermission().stream())
                .map(permission -> permission.getPermissionId())
                .collect(Collectors.toSet());
    }
}

package com.graduration.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.graduration.Constain.PermissionConstain;
import com.graduration.Constain.RoleConstain;
import com.graduration.DTO.Request.RegisterStudentRequest;
import com.graduration.DTO.Response.RegisterStudentResponse;
import com.graduration.entity.PermissionEntity;
import com.graduration.entity.Roles;
import com.graduration.entity.StudentEntity;
import com.graduration.entity.UserEntity;

class StudentMapperTest {
    private final UserMaper userMaper = new UserMaperImpl();

    @Test
    void mapsRegisterRequestToUserAndStudentEntities() {
        RegisterStudentRequest request = RegisterStudentRequest.builder()
                .userName("student01")
                .password("password")
                .studentCode("SV001")
                .fullName("Nguyen Van A")
                .email("student@example.com")
                .phone("0901234567")
                .classId(10L)
                .build();

        UserEntity user = userMaper.toUserEntity(request);
        StudentEntity student = userMaper.toStudentEntity(request);

        assertEquals("student01", user.getUserName());
        assertEquals("password", user.getPassword());
        assertEquals("SV001", student.getStudentCode());
        assertEquals("Nguyen Van A", student.getFullNameStudent());
        assertEquals("0901234567", student.getPhoneStudent());
        assertEquals("student@example.com", student.getEmail());
        assertNotNull(student.getClassEntity());
        assertEquals(10L, student.getClassEntity().getClassId());
    }

    @Test
    void mapsUserAndStudentEntitiesToRegisterResponse() {
        PermissionEntity permission = PermissionEntity.builder()
                .permissionId(PermissionConstain.user_read)
                .build();
        Roles role = Roles.builder()
                .role(RoleConstain.STUDENT)
                .permission(Set.of(permission))
                .build();
        UserEntity user = UserEntity.builder()
                .userId("user-id")
                .userName("student01")
                .createAt(LocalDateTime.of(2026, 8, 5, 10, 30))
                .roles(Set.of(role))
                .build();
        StudentEntity student = StudentEntity.builder()
                .studentCode("SV001")
                .fullNameStudent("Nguyen Van A")
                .phoneStudent("0901234567")
                .email("student@example.com")
                .build();

        RegisterStudentResponse response = userMaper.toStudentResponse(user, student);

        assertEquals("user-id", response.getIdUser());
        assertEquals("student01", response.getUserName());
        assertEquals("SV001", response.getStudentCode());
        assertEquals("Nguyen Van A", response.getFullName());
        assertEquals("2026-08-05", response.getCreateAt().toString());
        assertEquals(Set.of(RoleConstain.STUDENT), response.getRoles());
        assertEquals(Set.of(PermissionConstain.user_read), response.getPermissions());
    }
}

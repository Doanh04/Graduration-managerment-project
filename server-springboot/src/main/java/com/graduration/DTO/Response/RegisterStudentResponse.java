package com.graduration.DTO.Response;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.graduration.Constain.PermissionConstain;
import com.graduration.Constain.RoleConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterStudentResponse {
    String idUser;
    String userName;
    String status;
    LocalDate createAt;
    String studentCode;
    String fullName;
    String phone;
    String email;
    Long classId;
    String classCode;

    @Builder.Default
    Set<RoleConstain> roles = new HashSet<>();

    @Builder.Default
    Set<PermissionConstain> permissions = new HashSet<>();
}

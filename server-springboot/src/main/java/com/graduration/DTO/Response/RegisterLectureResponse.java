package com.graduration.DTO.Response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.graduration.Constain.PermissionConstain;
import com.graduration.Constain.RoleConstain;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterLectureResponse {
    String userId;
    String userName;
    String lecturerCode;
    String fullName;
    String degree;
    String email;
    String phone;
    String status;
    LocalDateTime createAt;

    @Builder.Default
    Set<RoleConstain> roles = new HashSet<>();

    @Builder.Default
    Set<PermissionConstain> permissions = new HashSet<>();
}

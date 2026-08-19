package com.graduration.DTO.Request;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.graduration.Constain.RoleConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateLecturerRequest {
    @Size(min = 4, max = 50, message = "INVALID_USERNAME")
    String userName;

    @Size(min = 8, max = 100, message = "INVALID_PASSWORD")
    String password;

    @Size(min = 1, max = 100, message = "LECTURER_NOT_BLANK")
    String lectureCode;

    @Size(min = 1, max = 255, message = "FULLNAME_NOT_BLANK")
    String fullName;

    String degree;

    @Email(message = "INVALID_EMAIL")
    String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "INVALID_PHONE")
    String phone;

    @Size(min = 1, message = "ROLE_NOT_BLANK")
    Set<RoleConstain> roles;
}

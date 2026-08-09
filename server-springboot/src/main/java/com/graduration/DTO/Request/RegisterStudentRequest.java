package com.graduration.DTO.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterStudentRequest {
    @NotBlank(message = "USERNAME_NOT_BLANK")
    @Size(min = 4, max = 50, message = "INVALID_USERNAME")
    String userName;

    @NotBlank(message = "PASSWORD_NOT_BLANK")
    @Size(min = 8, max = 100, message = "INVALID_PASSWORD")
    String password;

    @NotBlank(message = "STUDENT_NOT_BLANK")
    @Size(max = 100, message = "SUTDENT_NOT_BLANK")
    String studentCode;

    @NotBlank(message = "FULLNAME_NOT_BLANK")
    @Size(max = 255, message = "FULLNAME_NOT_BLANK")
    String fullName;

    @Email(message = "INVALID_EMAIL")
    String email;

    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "INVALID_PHONE")
    String phone;

    @NotBlank(message = "CLASS_NOT_BLANK")
    Long classId;
}

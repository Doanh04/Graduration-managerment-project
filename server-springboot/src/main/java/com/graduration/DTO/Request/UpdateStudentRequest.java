package com.graduration.DTO.Request;

import jakarta.validation.constraints.NotBlank;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateStudentRequest {
    @NotBlank(message = "USERNAME_NOT_BLANK")
    String userName;

    @NotBlank(message = "STUDENT_NOT_BLANK")
    String studentCode;

    @NotBlank(message = "FULLNAME_NOT_BLANK")
    String fullName;

    String email;
    String phone;
    Long classId;
}

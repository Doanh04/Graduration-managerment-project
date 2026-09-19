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

    /** Mã lớp dùng để tra cứu lớp học khi cập nhật hồ sơ sinh viên. */
    String classCode;

    /** Trường cũ chỉ được dùng để tương thích với client chưa nâng cấp. */
    @Deprecated
    Long classId;
}

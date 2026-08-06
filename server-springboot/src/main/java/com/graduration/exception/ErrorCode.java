package com.graduration.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {
    //    Lỗi cấu hình
    UKNOWN_ERROR(9999, "Unknown error", HttpStatus.INTERNAL_SERVER_ERROR),
    PERMISSION_INVALID(1001, "Permission Invalid", HttpStatus.NOT_FOUND),
    PERMISION_NAME_INVALID(1002, "Name Perission Invalid", HttpStatus.NOT_FOUND),
    PERMISSION_NOT_BLANK(1003, "Permission Not Blank", HttpStatus.BAD_REQUEST),
    PERMISSION_IS_EXITED(1004, "Permission is exited", HttpStatus.CONFLICT),
    ROLE_INVALID(1005, "Role Invalid", HttpStatus.NOT_FOUND),
    ROLE_NAME_INVALID(1006, "Role Name Invalid", HttpStatus.NOT_FOUND),
    ROLE_NOT_BLANK(1007, "Role is not blank", HttpStatus.BAD_REQUEST),
    ROLE_IS_EXITED(1008, "Role is exited", HttpStatus.CONFLICT),
    ROLE_NOT_FOUND(1009, "role not found", HttpStatus.CONFLICT),
    PERMISSION_NOT_FOUND(1010, "Permission not found", HttpStatus.CONFLICT),
    STATUS_NOT_FOUND(1011, "Status not found", HttpStatus.CONFLICT),
    INVALID_KEY(1012, "Uncategorized error", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1013, "Invalid password", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(1014, "invalid email", HttpStatus.BAD_REQUEST),
    EMAIL_IS_REQUIRED(1015, "email is required", HttpStatus.BAD_REQUEST),
    USERNAME_IS_EXITED(1016, "User name is exited", HttpStatus.BAD_REQUEST),
    EMAIL_VERIFIED_EXITED(1017, "Email is exited", HttpStatus.BAD_REQUEST),
    USER_EXITED(1018, "User is exited", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1019, "Authentication is required or token is invalid", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1020, "UserName or password incorrect", HttpStatus.FORBIDDEN),
    USER_NOT_FOUND(1021, "do not find user with user name", HttpStatus.NOT_FOUND),
    StATUS_BANNED(1022, "state already implemented", HttpStatus.BAD_REQUEST),
    OTP_INVALID(1023, "OTP Invalid", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1024, "OTP expired", HttpStatus.BAD_REQUEST),
    INVALID_USERNAME(1025, "Invalid username", HttpStatus.BAD_REQUEST),
    ACCESS_DENIED(1026, "You do not have permission", HttpStatus.FORBIDDEN),
    PASSWORD_NOT_BLANK(1027, "password not blank", HttpStatus.BAD_REQUEST),
    USERNAME_NOT_BLANK(1028, "username not blank", HttpStatus.BAD_REQUEST),
    LECTURER_NOT_BLANK(1029, "lecture not blank", HttpStatus.BAD_REQUEST),
    FULLNAME_NOT_BLANK(1030, "full name lecturer not blank", HttpStatus.BAD_REQUEST),
    INVALID_PHONE(1031, "invalid phone number", HttpStatus.BAD_REQUEST),
    PHONE_IS_EXITED(1032, "phone number is exited", HttpStatus.CONFLICT),
    LECTURER_CODE_IS_EXITED(1033, "lecturer code is exited", HttpStatus.CONFLICT),
    INVALID_EXCEL_FILE(1034, "invalid or empty Excel file", HttpStatus.BAD_REQUEST),
    EXCEL_ROW_INVALID(1035, "Excel row data is invalid", HttpStatus.BAD_REQUEST),
    STUDENT_NOT_BLANK(1036, "Student not blank", HttpStatus.BAD_REQUEST),
    CLASS_CODE_NOT_BLANK(1037, "Class Code not blank", HttpStatus.BAD_REQUEST),
    CLASS_NAME_NOT_BLANK(1038, "Class Name not blank", HttpStatus.BAD_REQUEST),
    MAJOR_ID_NOT_BLANK(1039, "Major id not blank", HttpStatus.BAD_REQUEST);

    ErrorCode(int code, String mesage, HttpStatusCode statusCode) {
        this.code = code;
        this.mesage = mesage;
        this.statusCode = statusCode;
    }

    int code;
    String mesage;
    HttpStatusCode statusCode;
}

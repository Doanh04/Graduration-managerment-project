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
    UNAUTHORIZED(1020, "Tên đăng nhập hoặc mật khẩu không chính xác", HttpStatus.FORBIDDEN),
    USER_NOT_FOUND(1021, "User not found by username, lecturer code or student code", HttpStatus.NOT_FOUND),
    StATUS_BANNED(1022, "state already implemented", HttpStatus.BAD_REQUEST),
    OTP_INVALID(1023, "OTP Invalid", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1024, "OTP expired", HttpStatus.BAD_REQUEST),
    INVALID_USERNAME(1025, "A username, lecturer code or student code is required", HttpStatus.BAD_REQUEST),
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
    MAJOR_ID_NOT_BLANK(1039, "Major id not blank", HttpStatus.BAD_REQUEST),
    AMBIGUOUS_LOGIN_IDENTIFIER(1040, "Login identifier matches multiple accounts", HttpStatus.CONFLICT),
    TEAM_NOT_FOUND(1041, "Team not found", HttpStatus.NOT_FOUND),
    TEAM_NAME_NOT_BLANK(1042, "Team name not blank", HttpStatus.BAD_REQUEST),
    TEAM_ALREADY_EXISTS(1043, "Team name already exists", HttpStatus.CONFLICT),
    STUDENT_ALREADY_IN_TEAM(1044, "Student already belongs to a team", HttpStatus.CONFLICT),
    TOPIC_ALREADY_ASSIGNED(1045, "Topic already belongs to another team", HttpStatus.CONFLICT),
    ACADEMIC_YEAR_NOT_FOUND(1046, "Academic year not found", HttpStatus.NOT_FOUND),
    ACADEMIC_YEAR_NOT_BLANK(1047, "Academic year not blank", HttpStatus.BAD_REQUEST),
    ACADEMIC_YEAR_INVALID(1048, "Academic year must have format YYYY-YYYY", HttpStatus.BAD_REQUEST),
    ACADEMIC_YEAR_ALREADY_EXISTS(1049, "Academic year already exists", HttpStatus.CONFLICT),
    ACADEMIC_YEAR_IN_USE(1050, "Academic year contains defense data and cannot be deleted", HttpStatus.CONFLICT),
    DEFENSE_PERIOD_NOT_FOUND(1051, "Defense period not found", HttpStatus.NOT_FOUND),
    DEFENSE_PERIOD_NAME_NOT_BLANK(1052, "Defense period name not blank", HttpStatus.BAD_REQUEST),
    DEFENSE_PERIOD_INVALID(1053, "Defense period data is invalid", HttpStatus.BAD_REQUEST),
    DEFENSE_PERIOD_INVALID_DATE(1054, "End date must not be before start date", HttpStatus.BAD_REQUEST),
    DEFENSE_PERIOD_ALREADY_EXISTS(1055, "Defense period already exists in this academic year", HttpStatus.CONFLICT),
    DEFENSE_PERIOD_IN_USE(
            1056, "Defense period contains topics or milestones and cannot be deleted", HttpStatus.CONFLICT),
    START_DATE_NOT_BLANK(1057, "Start date not blank", HttpStatus.BAD_REQUEST),
    END_DATE_NOT_BLANK(1058, "End date not blank", HttpStatus.BAD_REQUEST),
    LIBRARY_TOPIC_NOT_FOUND(1059, "Library topic not found", HttpStatus.NOT_FOUND),
    LIBRARY_TOPIC_TITLE_NOT_BLANK(1060, "Library topic title not blank", HttpStatus.BAD_REQUEST),
    LIBRARY_TOPIC_ALREADY_EXISTS(1061, "Library topic title already exists", HttpStatus.CONFLICT),
    TEMPLATE_NOT_FOUND(1062, "Template not found", HttpStatus.NOT_FOUND),
    TEMPLATE_NAME_NOT_BLANK(1063, "Template name not blank", HttpStatus.BAD_REQUEST),
    TEMPLATE_ALREADY_EXISTS(1064, "Template name already exists", HttpStatus.CONFLICT),
    AUDIT_LOG_NOT_FOUND(1065, "Audit log not found", HttpStatus.NOT_FOUND),
    AUDIT_LOG_ACTION_NOT_BLANK(1066, "Audit log action not blank", HttpStatus.BAD_REQUEST),
    AUDIT_LOG_USER_ID_NOT_BLANK(1067, "Audit log user id not blank", HttpStatus.BAD_REQUEST),
    CLASS_IN_USE(1068, "Class contains students and cannot be deleted", HttpStatus.CONFLICT),
    MAJOR_IN_USE(1069, "Major contains classes and cannot be deleted", HttpStatus.CONFLICT),
    ENDPOINT_NOT_FOUND(1070, "Endpoint not found", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(1071, "HTTP method not allowed", HttpStatus.METHOD_NOT_ALLOWED),
    DATA_CONFLICT(1072, "Data conflicts with an existing record", HttpStatus.CONFLICT),
    ACCOUNT_INACTIVE(1073, "Tài khoản đã dừng hoạt động", HttpStatus.UNAUTHORIZED);

    ErrorCode(int code, String mesage, HttpStatusCode statusCode) {
        this.code = code;
        this.mesage = mesage;
        this.statusCode = statusCode;
    }

    int code;
    String mesage;
    HttpStatusCode statusCode;
}

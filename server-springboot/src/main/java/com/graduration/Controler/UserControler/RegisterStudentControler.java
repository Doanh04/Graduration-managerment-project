package com.graduration.Controler.UserControler;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.graduration.DTO.Request.RegisterStudentRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.PasswordResetResponse;
import com.graduration.DTO.Response.RegisterStudentResponse;
import com.graduration.Service.UserService.UserStudentService;
import com.graduration.Service.UserService.UserStudentService.ImportStudentResult;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/register-student")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RegisterStudentControler {
    UserStudentService userStudentService;

    @PostMapping("/create-user")
    public ApiResponse<RegisterStudentResponse> registerStudent(@RequestBody RegisterStudentRequest request) {
        return ApiResponse.<RegisterStudentResponse>builder()
                .message("Student account registered successfully")
                .result(userStudentService.registerStudent(request))
                .build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportStudentResult> importStudents(@RequestPart("file") MultipartFile file) {
        return ApiResponse.<ImportStudentResult>builder()
                .message("Student accounts imported successfully")
                .result(userStudentService.importStudents(file))
                .build();
    }

    @GetMapping("/get-all-student")
    public ApiResponse<com.graduration.DTO.Response.PageResponse<RegisterStudentResponse>> getAllStudents(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer size) {
        return ApiResponse.<com.graduration.DTO.Response.PageResponse<RegisterStudentResponse>>builder()
                .result(userStudentService.getAllStudentsPage(page, size))
                .build();
    }

    @GetMapping("/{userName}")
    public ApiResponse<RegisterStudentResponse> getStudentByUserName(@PathVariable String userName) {
        return ApiResponse.<RegisterStudentResponse>builder()
                .result(userStudentService.getStudentByUserName(userName))
                .build();
    }

    @PatchMapping("/reset-password/{userName}")
    public ApiResponse<PasswordResetResponse> resetPassword(@PathVariable String userName) {
        return ApiResponse.<PasswordResetResponse>builder()
                .message("Student password reset successfully")
                .result(userStudentService.resetPasswordByUserName(userName))
                .build();
    }

    @DeleteMapping("/username/{userName}")
    public ApiResponse<Void> deleteStudentAccount(@PathVariable String userName) {
        userStudentService.deleteStudentAccount(userName);
        return ApiResponse.<Void>builder()
                .message("Student account deleted successfully")
                .build();
    }
}

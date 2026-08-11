package com.graduration.Controler.UserControler;

import java.util.List;

import jakarta.validation.Valid;

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

import com.graduration.DTO.Request.RegisterLectureRequest;
import com.graduration.DTO.Request.UpdateLecturerRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.ImportLectureResponse;
import com.graduration.DTO.Response.PasswordResetResponse;
import com.graduration.DTO.Response.RegisterLectureResponse;
import com.graduration.Service.UserService.UserLecturerService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/register-lecture")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RegisterLecturerControler {
    UserLecturerService userLecturerService;

    @PostMapping("/create-user")
    public ApiResponse<RegisterLectureResponse> registerLecturer(@Valid @RequestBody RegisterLectureRequest request) {
        return ApiResponse.<RegisterLectureResponse>builder()
                .message("Lecturer account registered successfully")
                .result(userLecturerService.registerLecturer(request))
                .build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportLectureResponse> importLecturers(@RequestPart("file") MultipartFile file) {
        return ApiResponse.<ImportLectureResponse>builder()
                .message("Lecturer accounts imported successfully")
                .result(userLecturerService.importLecturers(file))
                .build();
    }

    @PatchMapping("/{userId}")
    public ApiResponse<RegisterLectureResponse> updateLecturer(
            @PathVariable String userId, @Valid @RequestBody UpdateLecturerRequest request) {
        return ApiResponse.<RegisterLectureResponse>builder()
                .message("Lecturer account updated successfully")
                .result(userLecturerService.updateLecturer(userId, request))
                .build();
    }

    @GetMapping("/get-all-lecture")
    public ApiResponse<List<RegisterLectureResponse>> getAllLecturers(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer size) {
        return ApiResponse.<List<RegisterLectureResponse>>builder()
                .result(
                        page == null && size == null
                                ? userLecturerService.getAllLecturers()
                                : userLecturerService.getAllLecturers(page, size))
                .build();
    }

    @GetMapping("/username/{userName}")
    public ApiResponse<RegisterLectureResponse> getLecturerByUserName(@PathVariable String userName) {
        return ApiResponse.<RegisterLectureResponse>builder()
                .result(userLecturerService.getLecturerByUserName(userName))
                .build();
    }

    @PatchMapping("/reset-password/{userName}")
    public ApiResponse<PasswordResetResponse> resetPassword(@PathVariable String userName) {
        return ApiResponse.<PasswordResetResponse>builder()
                .message("Lecturer password reset successfully")
                .result(userLecturerService.resetPasswordByUserName(userName))
                .build();
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteLecturerAccount(@PathVariable String userId) {
        userLecturerService.deleteLecturerAccount(userId);
        return ApiResponse.<Void>builder()
                .message("Lecturer account deleted successfully")
                .build();
    }
}

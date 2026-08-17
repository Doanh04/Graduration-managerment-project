package com.graduration.Controler.Department;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.graduration.DTO.Request.ClassRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.ClassResponse;
import com.graduration.Service.DerpatmentService.ClassService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/class")
@RequiredArgsConstructor
public class ClassControler {
    private final ClassService classService;

    @PostMapping("/create-class")
    public ApiResponse<ClassResponse> createClass(@Valid @RequestBody ClassRequest request) {
        return ApiResponse.<ClassResponse>builder()
                .message("Class created successfully")
                .result(classService.createClass(request))
                .build();
    }

    @GetMapping("/{classCode}")
    public ApiResponse<ClassResponse> getClass(@PathVariable String classCode) {
        return ApiResponse.<ClassResponse>builder()
                .result(classService.getClass(classCode))
                .build();
    }

    @GetMapping("/get-all-class")
    public ApiResponse<com.graduration.DTO.Response.PageResponse<ClassResponse>> getAllClasses(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer size) {
        return ApiResponse.<com.graduration.DTO.Response.PageResponse<ClassResponse>>builder()
                .result(classService.getAllClassesPage(page, size))
                .build();
    }

    @PutMapping("/{classId}")
    public ApiResponse<ClassResponse> updateClass(
            @PathVariable Long classId, @Valid @RequestBody ClassRequest request) {
        return ApiResponse.<ClassResponse>builder()
                .message("Class updated successfully")
                .result(classService.updateClass(classId, request))
                .build();
    }

    @DeleteMapping("/{classId}")
    public ApiResponse<Void> deleteClass(@PathVariable Long classId) {
        classService.deleteClass(classId);
        return ApiResponse.<Void>builder().message("Class deleted successfully").build();
    }
}

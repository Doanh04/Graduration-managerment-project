package com.graduration.Controler.Department;

import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.graduration.DTO.Request.TeamRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.TeamResponse;
import com.graduration.Service.DerpatmentService.TeamService;
import com.graduration.Service.DerpatmentService.TeamService.ImportTeamStudentsResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/team")
@RequiredArgsConstructor
public class TeamControler {
    private final TeamService teamService;

    @PostMapping("/create-team")
    public ApiResponse<TeamResponse> createTeam(@Valid @RequestBody TeamRequest request) {
        return ApiResponse.<TeamResponse>builder()
                .message("Team created successfully")
                .result(teamService.createTeam(request))
                .build();
    }

    @GetMapping("/{teamId}")
    public ApiResponse<TeamResponse> getTeam(@PathVariable Long teamId) {
        return ApiResponse.<TeamResponse>builder()
                .result(teamService.getTeam(teamId))
                .build();
    }

    @GetMapping("/get-all-team")
    public ApiResponse<List<TeamResponse>> getAllTeams(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer size) {
        return ApiResponse.<List<TeamResponse>>builder()
                .result(page == null && size == null ? teamService.getAllTeams() : teamService.getAllTeams(page, size))
                .build();
    }

    @PutMapping("/{teamId}")
    public ApiResponse<TeamResponse> updateTeam(@PathVariable Long teamId, @Valid @RequestBody TeamRequest request) {
        return ApiResponse.<TeamResponse>builder()
                .message("Team updated successfully")
                .result(teamService.updateTeam(teamId, request))
                .build();
    }

    @DeleteMapping("/{teamId}")
    public ApiResponse<Void> deleteTeam(@PathVariable Long teamId) {
        teamService.deleteTeam(teamId);
        return ApiResponse.<Void>builder().message("Team deleted successfully").build();
    }

    @PostMapping("/{teamId}/students/{studentCode}")
    public ApiResponse<TeamResponse> addStudent(@PathVariable Long teamId, @PathVariable String studentCode) {
        return ApiResponse.<TeamResponse>builder()
                .message("Student added to team successfully")
                .result(teamService.addStudent(teamId, studentCode))
                .build();
    }

    @PostMapping("/{teamId}/students")
    public ApiResponse<TeamResponse> addStudents(@PathVariable Long teamId, @RequestBody Set<String> studentCodes) {
        return ApiResponse.<TeamResponse>builder()
                .message("Students added to team successfully")
                .result(teamService.addStudents(teamId, studentCodes))
                .build();
    }

    @DeleteMapping("/{teamId}/students/{studentCode}")
    public ApiResponse<TeamResponse> removeStudent(@PathVariable Long teamId, @PathVariable String studentCode) {
        return ApiResponse.<TeamResponse>builder()
                .message("Student removed from team successfully")
                .result(teamService.removeStudent(teamId, studentCode))
                .build();
    }

    @PostMapping(value = "/{teamId}/students/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportTeamStudentsResponse> importStudents(
            @PathVariable Long teamId, @RequestPart("file") MultipartFile file) {
        return ApiResponse.<ImportTeamStudentsResponse>builder()
                .message("Students imported into team")
                .result(teamService.importStudents(teamId, file))
                .build();
    }
}

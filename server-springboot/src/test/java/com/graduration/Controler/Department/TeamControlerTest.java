package com.graduration.Controler.Department;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.graduration.DTO.Request.TeamRequest;
import com.graduration.DTO.Response.TeamResponse;
import com.graduration.Service.DerpatmentService.TeamService;
import com.graduration.Service.DerpatmentService.TeamService.ImportTeamStudentsResponse;
import com.graduration.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class TeamControlerTest {
    MockMvc mockMvc;

    @Mock
    TeamService teamService;

    @InjectMocks
    TeamControler teamControler;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(teamControler)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createTeam_returnsCreatedTeam() throws Exception {
        when(teamService.createTeam(any(TeamRequest.class))).thenReturn(response());

        mockMvc.perform(
                        post("/team/create-team")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
								{"nameTeam":"Team 01","description":"Graduation team","joinDate":"2026-08-11"}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Team created successfully"))
                .andExpect(jsonPath("$.result.idTeam").value(1))
                .andExpect(jsonPath("$.result.nameTeam").value("Team 01"));
    }

    @Test
    void addMultipleStudents_acceptsStudentCodeArray() throws Exception {
        when(teamService.addStudents(eq(1L), any())).thenReturn(response());

        mockMvc.perform(post("/team/1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"SV001\",\"SV002\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Students added to team successfully"));

        verify(teamService).addStudents(1L, Set.of("SV001", "SV002"));
    }

    @Test
    void getTeam_returnsStudentSummaries() throws Exception {
        when(teamService.getTeam(1L)).thenReturn(response());

        mockMvc.perform(get("/team/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.students[0].studentCode").value("SV001"));
    }

    @Test
    void getAllTeams_returnsTeamList() throws Exception {
        when(teamService.getAllTeams()).thenReturn(List.of(response()));

        mockMvc.perform(get("/team/get-all-team"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1))
                .andExpect(jsonPath("$.result[0].idTeam").value(1));
    }

    @Test
    void updateTeam_returnsUpdatedTeam() throws Exception {
        when(teamService.updateTeam(eq(1L), any(TeamRequest.class))).thenReturn(response());

        mockMvc.perform(put("/team/1").contentType(MediaType.APPLICATION_JSON).content("{\"nameTeam\":\"Team 01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Team updated successfully"));

        verify(teamService).updateTeam(eq(1L), any(TeamRequest.class));
    }

    @Test
    void deleteTeam_returnsSuccess() throws Exception {
        mockMvc.perform(delete("/team/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Team deleted successfully"));

        verify(teamService).deleteTeam(1L);
    }

    @Test
    void addStudent_usesStudentCodePathVariable() throws Exception {
        when(teamService.addStudent(1L, "SV001")).thenReturn(response());

        mockMvc.perform(post("/team/1/students/SV001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Student added to team successfully"));

        verify(teamService).addStudent(1L, "SV001");
    }

    @Test
    void removeStudent_usesStudentCodePathVariable() throws Exception {
        when(teamService.removeStudent(1L, "SV001")).thenReturn(response());

        mockMvc.perform(delete("/team/1/students/SV001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Student removed from team successfully"));

        verify(teamService).removeStudent(1L, "SV001");
    }

    @Test
    void importStudents_acceptsExcelFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[] {1});
        when(teamService.importStudents(eq(1L), any()))
                .thenReturn(new ImportTeamStudentsResponse(1, 1, 0, List.of(), List.of()));

        mockMvc.perform(multipart("/team/1/students/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalRows").value(1))
                .andExpect(jsonPath("$.result.successRows").value(1));
    }

    @Test
    void createTeam_rejectsBlankNameBeforeService() throws Exception {
        mockMvc.perform(post("/team/create-team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nameTeam\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    private TeamResponse response() {
        return TeamResponse.builder()
                .idTeam(1L)
                .nameTeam("Team 01")
                .students(List.of(TeamResponse.StudentSummary.builder()
                        .studentCode("SV001")
                        .fullName("Student One")
                        .build()))
                .build();
    }
}

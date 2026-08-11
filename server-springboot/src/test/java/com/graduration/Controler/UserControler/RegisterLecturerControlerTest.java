package com.graduration.Controler.UserControler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.graduration.Constain.PermissionConstain;
import com.graduration.Constain.RoleConstain;
import com.graduration.DTO.Request.RegisterLectureRequest;
import com.graduration.DTO.Request.UpdateLecturerRequest;
import com.graduration.DTO.Response.ImportLectureResponse;
import com.graduration.DTO.Response.PasswordResetResponse;
import com.graduration.DTO.Response.RegisterLectureResponse;
import com.graduration.Service.UserService.UserLecturerService;
import com.graduration.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class RegisterLecturerControlerTest {
    MockMvc mockMvc;

    @Mock
    UserLecturerService userLecturerService;

    @InjectMocks
    RegisterLecturerControler registerLecturerControler;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(registerLecturerControler)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerLecturer_returnsRegisteredAccount() throws Exception {
        when(userLecturerService.registerLecturer(any(RegisterLectureRequest.class)))
                .thenReturn(registerResponse());

        mockMvc.perform(
                        post("/register-lecture/create-user")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
								{
								"userName": "lecturer01",
								"password": "password123",
								"lectureCode": "GV001",
								"fullName": "Nguyen Van A",
								"degree": "Master",
								"email": "lecturer@example.com",
								"phone": "0901234567"
								}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Lecturer account registered successfully"))
                .andExpect(jsonPath("$.result.userName").value("lecturer01"))
                .andExpect(jsonPath("$.result.status").value("ACTIVE"))
                .andExpect(jsonPath("$.result.roles[0]").value("SUPERVISOR"))
                .andExpect(jsonPath("$.result.permissions[0]").value("topic_read"));
    }

    @Test
    void registerLecturer_rejectsInvalidRequestBeforeCallingService() throws Exception {
        mockMvc.perform(
                        post("/register-lecture/create-user")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
								{
								"userName": "abc",
								"password": "short",
								"lectureCode": "",
								"fullName": ""
								}
								"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").isNumber());

        verify(userLecturerService, org.mockito.Mockito.never()).registerLecturer(any(RegisterLectureRequest.class));
    }

    @Test
    void importLecturers_returnsImportSummary() throws Exception {
        ImportLectureResponse importResponse = ImportLectureResponse.builder()
                .totalRows(2)
                .successRows(1)
                .failedRows(1)
                .importedLecturers(List.of(registerResponse()))
                .build();
        when(userLecturerService.importLecturers(any())).thenReturn(importResponse);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "lecturers.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/register-lecture/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.totalRows").value(2))
                .andExpect(jsonPath("$.result.successRows").value(1))
                .andExpect(jsonPath("$.result.failedRows").value(1));
    }

    @Test
    void updateLecturer_updatesOnlyProvidedFields() throws Exception {
        RegisterLectureResponse response = registerResponse();
        response.setFullName("Updated Name");
        when(userLecturerService.updateLecturer(
                        org.mockito.ArgumentMatchers.eq("user-1"), any(UpdateLecturerRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/register-lecture/user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{
								"fullName": "Updated Name"
								}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lecturer account updated successfully"))
                .andExpect(jsonPath("$.result.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.result.userName").value("lecturer01"));
    }

    @Test
    void updateLecturer_updatesRolesAndReturnsPermissions() throws Exception {
        RegisterLectureResponse response = registerResponse();
        response.setRoles(Set.of(RoleConstain.SUPERVISOR, RoleConstain.REVIEWER));
        response.setPermissions(Set.of(PermissionConstain.topic_read, PermissionConstain.score_read));
        when(userLecturerService.updateLecturer(
                        org.mockito.ArgumentMatchers.eq("user-1"), any(UpdateLecturerRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/register-lecture/user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{
								"roles": ["SUPERVISOR", "REVIEWER"]
								}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.roles.length()").value(2))
                .andExpect(jsonPath("$.result.permissions.length()").value(2));
    }

    @Test
    void getAllLecturers_returnsLecturerList() throws Exception {
        when(userLecturerService.getAllLecturers()).thenReturn(List.of(registerResponse()));

        mockMvc.perform(get("/register-lecture/get-all-lecture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.length()").value(1))
                .andExpect(jsonPath("$.result[0].userName").value("lecturer01"))
                .andExpect(jsonPath("$.result[0].roles[0]").value("SUPERVISOR"));
    }

    @Test
    void getLecturerByUserName_returnsLecturer() throws Exception {
        when(userLecturerService.getLecturerByUserName("lecturer01")).thenReturn(registerResponse());

        mockMvc.perform(get("/register-lecture/username/lecturer01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.userName").value("lecturer01"))
                .andExpect(jsonPath("$.result.lecturerCode").value("GV001"));

        verify(userLecturerService).getLecturerByUserName("lecturer01");
    }

    @Test
    void resetPassword_returnsSuccessResponse() throws Exception {
        when(userLecturerService.resetPasswordByUserName("lecturer01"))
                .thenReturn(PasswordResetResponse.builder()
                        .userName("lecturer01")
                        .temporaryPassword("temporary-password")
                        .build());

        mockMvc.perform(patch("/register-lecture/reset-password/lecturer01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Lecturer password reset successfully"))
                .andExpect(jsonPath("$.result.temporaryPassword").value("temporary-password"));

        verify(userLecturerService).resetPasswordByUserName("lecturer01");
    }

    @Test
    void deleteLecturerAccount_returnsSuccessResponse() throws Exception {
        doNothing().when(userLecturerService).deleteLecturerAccount("user-1");

        mockMvc.perform(delete("/register-lecture/user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Lecturer account deleted successfully"));

        verify(userLecturerService).deleteLecturerAccount("user-1");
    }

    private RegisterLectureResponse registerResponse() {
        return RegisterLectureResponse.builder()
                .userId("user-1")
                .userName("lecturer01")
                .lecturerCode("GV001")
                .fullName("Nguyen Van A")
                .status("ACTIVE")
                .roles(Set.of(RoleConstain.SUPERVISOR))
                .permissions(Set.of(PermissionConstain.topic_read))
                .build();
    }
}

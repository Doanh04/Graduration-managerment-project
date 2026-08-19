package com.graduration.Controler.UserControler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.graduration.Constain.PermissionConstain;
import com.graduration.DTO.Request.CreatePermissionRequest;
import com.graduration.DTO.Request.UpdatePermissionRequest;
import com.graduration.DTO.Response.PermissionResponse;
import com.graduration.Service.UserService.PermissionService;
import com.graduration.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class PermissionControlerTest {
    MockMvc mockMvc;

    @Mock
    PermissionService permissionService;

    @InjectMocks
    PermissionControler permissionControler;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(permissionControler)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createPermission_returnsCreatedPermission() throws Exception {
        when(permissionService.createPermission(any(CreatePermissionRequest.class)))
                .thenReturn(permissionResponse());

        mockMvc.perform(
                        post("/permission")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
								{
								"permissionId": "topic_read",
								"permissionName": "Read topic",
								"description": "View topics"
								}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Permission created successfully"))
                .andExpect(jsonPath("$.result.permissionId").value("topic_read"));
    }

    @Test
    void updatePermission_returnsUpdatedPermission() throws Exception {
        when(permissionService.updatePermission(any(PermissionConstain.class), any(UpdatePermissionRequest.class)))
                .thenReturn(permissionResponse());

        mockMvc.perform(
                        put("/permission/topic_read")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
								{
								"permissionName": "Read topic",
								"description": "Updated description"
								}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Permission updated successfully"))
                .andExpect(jsonPath("$.result.permissionName").value("Read topic"));
    }

    @Test
    void deletePermission_returnsSuccessResponse() throws Exception {
        doNothing().when(permissionService).deletePermission(PermissionConstain.topic_read);

        mockMvc.perform(delete("/permission/topic_read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Permission deleted successfully"));

        verify(permissionService).deletePermission(PermissionConstain.topic_read);
    }

    @Test
    void getPermission_returnsPermission() throws Exception {
        when(permissionService.getPermission(PermissionConstain.topic_read)).thenReturn(permissionResponse());

        mockMvc.perform(get("/permission/topic_read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.permissionId").value("topic_read"));
    }

    @Test
    void getAllPermissions_returnsPermissionList() throws Exception {
        when(permissionService.getAllPermissionsPage(null, null))
                .thenReturn(com.graduration.DTO.Response.PageResponse.of(List.of(permissionResponse())));

        mockMvc.perform(get("/permission"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content.length()").value(1))
                .andExpect(jsonPath("$.result.content[0].permissionId").value("topic_read"));
    }

    private PermissionResponse permissionResponse() {
        return PermissionResponse.builder()
                .permissionId(PermissionConstain.topic_read)
                .permissionName("Read topic")
                .description("View topics")
                .build();
    }
}

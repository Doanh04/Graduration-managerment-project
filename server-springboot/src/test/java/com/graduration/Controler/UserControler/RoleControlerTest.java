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
import java.util.Set;

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
import com.graduration.Constain.RoleConstain;
import com.graduration.Constain.RoleNameConstain;
import com.graduration.DTO.Request.CreateRoleRequest;
import com.graduration.DTO.Request.UpdateRoleRequest;
import com.graduration.DTO.Response.RoleResponse;
import com.graduration.Service.RoleService.RoleService;
import com.graduration.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class RoleControlerTest {
    MockMvc mockMvc;

    @Mock
    RoleService roleService;

    @InjectMocks
    RoleControler roleControler;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roleControler)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createRole_returnsCreatedRole() throws Exception {
        when(roleService.createRole(any(CreateRoleRequest.class))).thenReturn(roleResponse());

        mockMvc.perform(
                        post("/role")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
								{
								"role": "SUPERVISOR",
								"roleName": "NAME_SUPERVISOR",
								"description": "Supervisor",
								"permissions": ["topic_read"]
								}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Role created successfully"))
                .andExpect(jsonPath("$.result.role").value("SUPERVISOR"))
                .andExpect(jsonPath("$.result.permissions[0]").value("topic_read"));
    }

    @Test
    void updateRole_returnsUpdatedRole() throws Exception {
        when(roleService.updateRole(any(RoleConstain.class), any(UpdateRoleRequest.class)))
                .thenReturn(roleResponse());

        mockMvc.perform(
                        put("/role/SUPERVISOR")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
								{
								"roleName": "NAME_SUPERVISOR",
								"description": "Updated supervisor",
								"permissions": ["topic_read"]
								}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.roleName").value("NAME_SUPERVISOR"));
    }

    @Test
    void deleteRole_returnsSuccessResponse() throws Exception {
        doNothing().when(roleService).deleteRole(RoleConstain.SUPERVISOR);

        mockMvc.perform(delete("/role/SUPERVISOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Role deleted successfully"));

        verify(roleService).deleteRole(RoleConstain.SUPERVISOR);
    }

    @Test
    void getRole_returnsRole() throws Exception {
        when(roleService.getRole(RoleConstain.SUPERVISOR)).thenReturn(roleResponse());

        mockMvc.perform(get("/role/SUPERVISOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.role").value("SUPERVISOR"));
    }

    @Test
    void getAllRoles_returnsRoleList() throws Exception {
        when(roleService.getAllRoles()).thenReturn(List.of(roleResponse()));

        mockMvc.perform(get("/role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1))
                .andExpect(jsonPath("$.result[0].role").value("SUPERVISOR"));
    }

    private RoleResponse roleResponse() {
        return RoleResponse.builder()
                .role(RoleConstain.SUPERVISOR)
                .roleName(RoleNameConstain.NAME_SUPERVISOR)
                .description("Supervisor")
                .permissions(Set.of(PermissionConstain.topic_read))
                .build();
    }
}

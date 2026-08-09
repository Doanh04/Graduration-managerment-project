package com.graduration.Controler.AuthenticationControler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.graduration.Constain.RoleConstain;
import com.graduration.DTO.Request.AuthenticationRequest;
import com.graduration.DTO.Request.IntrospectRequest;
import com.graduration.DTO.Request.LogoutRequest;
import com.graduration.DTO.Request.RefreshRequest;
import com.graduration.DTO.Response.AuthenticationResponse;
import com.graduration.DTO.Response.IntrospectResponse;
import com.graduration.Service.AuthenticationService.AuthenticationService;
import com.graduration.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class AuthenticationTest {
    MockMvc mockMvc;

    @Mock
    AuthenticationService authenticationService;

    @InjectMocks
    Authentication authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authentication)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_returnsAuthenticationResponse() throws Exception {
        when(authenticationService.authenticate(any(AuthenticationRequest.class)))
                .thenReturn(AuthenticationResponse.builder()
                        .token("jwt-token")
                        .authenticated(true)
                        .accountType("STUDENT")
                        .roles(Set.of(RoleConstain.STUDENT))
                        .build());

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
								{
								"userName": "student01",
								"password": "password123"
								}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.result.token").value("jwt-token"))
                .andExpect(jsonPath("$.result.authenticated").value(true))
                .andExpect(jsonPath("$.result.accountType").value("STUDENT"))
                .andExpect(jsonPath("$.result.roles[0]").value("STUDENT"));
    }

    @Test
    void introspect_returnsTokenValidity() throws Exception {
        when(authenticationService.introspect(any(IntrospectRequest.class)))
                .thenReturn(IntrospectResponse.builder().valid(true).build());

        mockMvc.perform(post("/auth/introspect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"jwt-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.valid").value(true));
    }

    @Test
    void refresh_returnsNewToken() throws Exception {
        when(authenticationService.refresh(any(RefreshRequest.class)))
                .thenReturn(AuthenticationResponse.builder()
                        .token("new-jwt-token")
                        .authenticated(true)
                        .accountType("LECTURER")
                        .roles(Set.of(RoleConstain.SUPERVISOR))
                        .build());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"old-jwt-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.result.token").value("new-jwt-token"))
                .andExpect(jsonPath("$.result.roles[0]").value("SUPERVISOR"));
    }

    @Test
    void logout_returnsSuccessResponse() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"jwt-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Logout successful"));

        verify(authenticationService).logout(any(LogoutRequest.class));
    }
}

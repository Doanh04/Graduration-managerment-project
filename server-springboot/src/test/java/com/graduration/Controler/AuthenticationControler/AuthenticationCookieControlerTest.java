package com.graduration.Controler.AuthenticationControler;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.graduration.Constain.RoleConstain;
import com.graduration.DTO.Request.AuthenticationRequest;
import com.graduration.DTO.Request.IntrospectRequest;
import com.graduration.DTO.Request.RefreshRequest;
import com.graduration.DTO.Response.AuthenticationResponse;
import com.graduration.DTO.Response.IntrospectResponse;
import com.graduration.Service.AuthenticationService.AuthenticationService;

@ExtendWith(MockitoExtension.class)
class AuthenticationCookieControlerTest {
    MockMvc mockMvc;

    @Mock
    AuthenticationService authenticationService;

    @InjectMocks
    AuthenticationCookieControler controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "validDuration", 3600L);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void login_setsHttpOnlyCookieWithoutReturningTokenInBody() throws Exception {
        when(authenticationService.authenticate(org.mockito.ArgumentMatchers.any(AuthenticationRequest.class)))
                .thenReturn(authenticationResponse("jwt-token", "STUDENT", RoleConstain.STUDENT));

        mockMvc.perform(post("/auth/cookie/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"student01\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                                HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("access_token=jwt-token")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("HttpOnly")))
                .andExpect(header().string(
                                HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("SameSite=Strict")))
                .andExpect(jsonPath("$.result.authenticated").value(true))
                .andExpect(jsonPath("$.result.roles[0]").value("STUDENT"))
                .andExpect(jsonPath("$.result.token").doesNotExist());
    }

    @Test
    void login_acceptsLecturerCodePayload() throws Exception {
        when(authenticationService.authenticate(org.mockito.ArgumentMatchers.any(AuthenticationRequest.class)))
                .thenReturn(authenticationResponse("jwt-token", "LECTURER", RoleConstain.SUPERVISOR));

        mockMvc.perform(post("/auth/cookie/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lecturerCode\":\"GV001\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accountType").value("LECTURER"));

        verify(authenticationService).authenticate(argThat(request -> "GV001".equals(request.getLecturerCode())));
    }

    @Test
    void introspect_readsTokenFromCookie() throws Exception {
        when(authenticationService.introspect(argThat(request -> "jwt-token".equals(request.getToken()))))
                .thenReturn(IntrospectResponse.builder().valid(true).build());

        mockMvc.perform(post("/auth/cookie/introspect").cookie(tokenCookie("jwt-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.valid").value(true));

        verify(authenticationService).introspect(org.mockito.ArgumentMatchers.any(IntrospectRequest.class));
    }

    @Test
    void refresh_replacesCookieWithoutReturningTokenInBody() throws Exception {
        when(authenticationService.refresh(argThat(request -> "old-token".equals(request.getToken()))))
                .thenReturn(authenticationResponse("new-token", "LECTURER", RoleConstain.SUPERVISOR));

        mockMvc.perform(post("/auth/cookie/refresh").cookie(tokenCookie("old-token")))
                .andExpect(status().isOk())
                .andExpect(header().string(
                                HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("access_token=new-token")))
                .andExpect(jsonPath("$.result.accountType").value("LECTURER"))
                .andExpect(jsonPath("$.result.token").doesNotExist());

        verify(authenticationService).refresh(org.mockito.ArgumentMatchers.any(RefreshRequest.class));
    }

    @Test
    void logout_invalidatesTokenAndClearsCookie() throws Exception {
        mockMvc.perform(post("/auth/cookie/logout").cookie(tokenCookie("jwt-token")))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("access_token=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")))
                .andExpect(jsonPath("$.message").value("Logout successful"));

        verify(authenticationService).logout(argThat(request -> "jwt-token".equals(request.getToken())));
    }

    private Cookie tokenCookie(String token) {
        return new Cookie(AuthenticationCookieControler.ACCESS_TOKEN_COOKIE, token);
    }

    private AuthenticationResponse authenticationResponse(String token, String accountType, RoleConstain role) {
        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .accountType(accountType)
                .roles(Set.of(role))
                .build();
    }
}

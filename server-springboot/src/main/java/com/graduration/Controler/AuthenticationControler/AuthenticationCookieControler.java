package com.graduration.Controler.AuthenticationControler;

import java.text.ParseException;
import java.time.Duration;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.graduration.DTO.Request.AuthenticationRequest;
import com.graduration.DTO.Request.IntrospectRequest;
import com.graduration.DTO.Request.LogoutRequest;
import com.graduration.DTO.Request.RefreshRequest;
import com.graduration.DTO.Response.ApiResponse;
import com.graduration.DTO.Response.AuthenticationResponse;
import com.graduration.DTO.Response.CookieAuthenticationResponse;
import com.graduration.DTO.Response.IntrospectResponse;
import com.graduration.Service.AuthenticationService.AuthenticationService;
import com.nimbusds.jose.JOSEException;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@RestController
@RequestMapping("/auth/cookie")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationCookieControler {
    static final String ACCESS_TOKEN_COOKIE = "access_token";

    AuthenticationService authenticationService;

    @NonFinal
    @Value("${jwt.valid-duration}")
    long validDuration;

    @PostMapping("/login")
    public ApiResponse<CookieAuthenticationResponse> login(
            @RequestBody AuthenticationRequest request, HttpServletResponse response) {
        AuthenticationResponse authentication = authenticationService.authenticate(request);
        addTokenCookie(response, authentication.getToken());

        return ApiResponse.<CookieAuthenticationResponse>builder()
                .message("Login successful")
                .result(toCookieResponse(authentication))
                .build();
    }

    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(@CookieValue(name = ACCESS_TOKEN_COOKIE) String token) {
        return ApiResponse.<IntrospectResponse>builder()
                .result(authenticationService.introspect(
                        IntrospectRequest.builder().token(token).build()))
                .build();
    }

    @PostMapping("/refresh")
    public ApiResponse<CookieAuthenticationResponse> refresh(
            @CookieValue(name = ACCESS_TOKEN_COOKIE) String token, HttpServletResponse response)
            throws ParseException, JOSEException {
        AuthenticationResponse authentication = authenticationService.refresh(
                RefreshRequest.builder().token(token).build());
        addTokenCookie(response, authentication.getToken());

        return ApiResponse.<CookieAuthenticationResponse>builder()
                .message("Token refreshed successfully")
                .result(toCookieResponse(authentication))
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@CookieValue(name = ACCESS_TOKEN_COOKIE) String token, HttpServletResponse response)
            throws ParseException, JOSEException {
        authenticationService.logout(LogoutRequest.builder().token(token).build());
        clearTokenCookie(response);

        return ApiResponse.<Void>builder().message("Logout successful").build();
    }

    private CookieAuthenticationResponse toCookieResponse(AuthenticationResponse authentication) {
        return CookieAuthenticationResponse.builder()
                .authenticated(authentication.isAuthenticated())
                .accountType(authentication.getAccountType())
                .roles(authentication.getRoles())
                .build();
    }

    private void addTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofSeconds(validDuration))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}

package com.graduration.Service.AuthenticationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.graduration.Constain.PermissionConstain;
import com.graduration.Constain.RoleConstain;
import com.graduration.Constain.StatusConstain;
import com.graduration.DTO.Request.AuthenticationRequest;
import com.graduration.DTO.Request.IntrospectRequest;
import com.graduration.DTO.Request.LogoutRequest;
import com.graduration.DTO.Request.RefreshRequest;
import com.graduration.DTO.Response.AuthenticationResponse;
import com.graduration.Repository.InvalidatedRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.InvalidatedToken;
import com.graduration.entity.LectureEntity;
import com.graduration.entity.PermissionEntity;
import com.graduration.entity.Roles;
import com.graduration.entity.StudentEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.AuthenticationMapper;
import com.nimbusds.jwt.SignedJWT;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    private static final String SIGNER_KEY = "0123456789012345678901234567890123456789";

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    InvalidatedRepository invalidatedRepository;

    @Spy
    AuthenticationMapper authenticationMapper = Mappers.getMapper(AuthenticationMapper.class);

    @InjectMocks
    AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService.SIGNER_KEY = SIGNER_KEY;
        authenticationService.VALID_DURATION = 3600;
        authenticationService.RERESHABLE_DURATION = 36000;
    }

    @Test
    void authenticate_studentReturnsTokenWithStudentClaimsAndScope() throws Exception {
        UserEntity student = studentUser();
        when(userRepository.findByUserNameOrLecture_LectureCodeOrStudent_StudentCode(
                        "student01", "student01", "student01"))
                .thenReturn(java.util.List.of(student));
        when(passwordEncoder.matches("password123", student.getPassword())).thenReturn(true);

        AuthenticationResponse response = authenticationService.authenticate(authenticationRequest(" student01 "));
        SignedJWT jwt = SignedJWT.parse(response.getToken());

        assertTrue(response.isAuthenticated());
        assertEquals("STUDENT", response.getAccountType());
        assertEquals(Set.of(RoleConstain.STUDENT), response.getRoles());
        assertEquals("student-user-id", jwt.getJWTClaimsSet().getSubject());
        assertEquals("student01", jwt.getJWTClaimsSet().getStringClaim("userName"));
        assertEquals("STUDENT", jwt.getJWTClaimsSet().getStringClaim("accountType"));
        assertEquals(Set.of("STUDENT"), Set.copyOf(jwt.getJWTClaimsSet().getStringListClaim("roles")));
        assertEquals("student@example.com", jwt.getJWTClaimsSet().getStringClaim("email"));
        assertEquals("0901234567", jwt.getJWTClaimsSet().getStringClaim("phone"));
        assertEquals("ACTIVE", jwt.getJWTClaimsSet().getStringClaim("status"));
        assertTrue(jwt.getJWTClaimsSet().getStringClaim("scope").contains("ROLE_STUDENT"));
        assertTrue(jwt.getJWTClaimsSet().getStringClaim("scope").contains("PERMISSION_user_read"));
        verify(userRepository)
                .findByUserNameOrLecture_LectureCodeOrStudent_StudentCode("student01", "student01", "student01");
    }

    @Test
    void authenticate_lecturerReturnsTokenWithLecturerClaims() throws Exception {
        UserEntity lecturer = lecturerUser();
        when(userRepository.findByUserNameOrLecture_LectureCodeOrStudent_StudentCode(
                        "lecturer01", "lecturer01", "lecturer01"))
                .thenReturn(java.util.List.of(lecturer));
        when(passwordEncoder.matches("password123", lecturer.getPassword())).thenReturn(true);

        AuthenticationResponse response = authenticationService.authenticate(authenticationRequest("lecturer01"));
        SignedJWT jwt = SignedJWT.parse(response.getToken());

        assertTrue(response.isAuthenticated());
        assertEquals("LECTURER", response.getAccountType());
        assertEquals(Set.of(RoleConstain.SUPERVISOR), response.getRoles());
        assertEquals("LECTURER", jwt.getJWTClaimsSet().getStringClaim("accountType"));
        assertEquals(Set.of("SUPERVISOR"), Set.copyOf(jwt.getJWTClaimsSet().getStringListClaim("roles")));
        assertEquals("lecturer@example.com", jwt.getJWTClaimsSet().getStringClaim("email"));
        assertEquals("0912345678", jwt.getJWTClaimsSet().getStringClaim("phone"));
        assertTrue(jwt.getJWTClaimsSet().getStringClaim("scope").contains("ROLE_SUPERVISOR"));
    }

    @Test
    void authenticate_studentCodeReturnsStudentAccount() {
        UserEntity student = studentUser();
        when(userRepository.findByUserNameOrLecture_LectureCodeOrStudent_StudentCode("SV001", "SV001", "SV001"))
                .thenReturn(java.util.List.of(student));
        when(passwordEncoder.matches("password123", student.getPassword())).thenReturn(true);

        AuthenticationResponse response = authenticationService.authenticate(AuthenticationRequest.builder()
                .studentCode(" SV001 ")
                .password("password123")
                .build());

        assertTrue(response.isAuthenticated());
        assertEquals("STUDENT", response.getAccountType());
    }

    @Test
    void authenticate_lecturerCodeReturnsLecturerAccount() {
        UserEntity lecturer = lecturerUser();
        when(userRepository.findByUserNameOrLecture_LectureCodeOrStudent_StudentCode("GV001", "GV001", "GV001"))
                .thenReturn(java.util.List.of(lecturer));
        when(passwordEncoder.matches("password123", lecturer.getPassword())).thenReturn(true);

        AuthenticationResponse response = authenticationService.authenticate(AuthenticationRequest.builder()
                .identifier("GV001")
                .password("password123")
                .build());

        assertTrue(response.isAuthenticated());
        assertEquals("LECTURER", response.getAccountType());
    }

    @Test
    void authenticate_conflictingIdentifiersThrowsInvalidUsername() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .userName("student01")
                .studentCode("SV001")
                .password("password123")
                .build();

        AppException exception = assertThrows(AppException.class, () -> authenticationService.authenticate(request));

        assertEquals(ErrorCode.INVALID_USERNAME, exception.getErrorCode());
    }

    @Test
    void authenticate_multipleMatchingAccountsThrowsAmbiguousIdentifier() {
        UserEntity student = studentUser();
        UserEntity lecturer = lecturerUser();
        when(userRepository.findByUserNameOrLecture_LectureCodeOrStudent_StudentCode(
                        "SHARED01", "SHARED01", "SHARED01"))
                .thenReturn(java.util.List.of(student, lecturer));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);

        AppException exception = assertThrows(
                AppException.class, () -> authenticationService.authenticate(authenticationRequest("SHARED01")));

        assertEquals(ErrorCode.AMBIGUOUS_LOGIN_IDENTIFIER, exception.getErrorCode());
    }

    @Test
    void authenticate_wrongPasswordThrowsUnauthorized() {
        UserEntity student = studentUser();
        when(userRepository.findByUserNameOrLecture_LectureCodeOrStudent_StudentCode(
                        "student01", "student01", "student01"))
                .thenReturn(java.util.List.of(student));
        when(passwordEncoder.matches("wrong-password", student.getPassword())).thenReturn(false);

        AppException exception = assertThrows(
                AppException.class,
                () -> authenticationService.authenticate(authenticationRequest("student01", "wrong-password")));

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void introspect_returnsTrueForValidTokenAndFalseForInvalidToken() {
        String token = authenticationService.generateToken(studentUser());
        when(invalidatedRepository.existsById(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(false);

        assertTrue(authenticationService
                .introspect(IntrospectRequest.builder().token(token).build())
                .isValid());
        assertFalse(authenticationService
                .introspect(IntrospectRequest.builder().token("invalid-token").build())
                .isValid());
    }

    @Test
    void refresh_invalidatesOldTokenAndReturnsNewToken() throws Exception {
        UserEntity student = studentUser();
        String oldToken = authenticationService.generateToken(student);
        SignedJWT oldJwt = SignedJWT.parse(oldToken);
        when(invalidatedRepository.existsById(oldJwt.getJWTClaimsSet().getJWTID()))
                .thenReturn(false);
        when(userRepository.findById("student-user-id")).thenReturn(Optional.of(student));

        AuthenticationResponse response = authenticationService.refresh(
                RefreshRequest.builder().token(oldToken).build());

        assertTrue(response.isAuthenticated());
        assertEquals("STUDENT", response.getAccountType());
        assertFalse(oldToken.equals(response.getToken()));

        ArgumentCaptor<InvalidatedToken> captor = ArgumentCaptor.forClass(InvalidatedToken.class);
        verify(invalidatedRepository).save(captor.capture());
        assertEquals(oldJwt.getJWTClaimsSet().getJWTID(), captor.getValue().getID());
    }

    @Test
    void logout_invalidatesToken() throws Exception {
        String token = authenticationService.generateToken(lecturerUser());
        SignedJWT jwt = SignedJWT.parse(token);
        when(invalidatedRepository.existsById(jwt.getJWTClaimsSet().getJWTID())).thenReturn(false);

        authenticationService.logout(LogoutRequest.builder().token(token).build());

        ArgumentCaptor<InvalidatedToken> captor = ArgumentCaptor.forClass(InvalidatedToken.class);
        verify(invalidatedRepository).save(captor.capture());
        assertEquals(jwt.getJWTClaimsSet().getJWTID(), captor.getValue().getID());
        assertEquals(
                jwt.getJWTClaimsSet().getExpirationTime(), captor.getValue().getExpiryTime());
    }

    private AuthenticationRequest authenticationRequest(String userName) {
        return authenticationRequest(userName, "password123");
    }

    private AuthenticationRequest authenticationRequest(String userName, String password) {
        return AuthenticationRequest.builder()
                .userName(userName)
                .password(password)
                .build();
    }

    private UserEntity studentUser() {
        PermissionEntity permission = PermissionEntity.builder()
                .permissionId(PermissionConstain.user_read)
                .build();
        Roles role = Roles.builder()
                .role(RoleConstain.STUDENT)
                .permission(Set.of(permission))
                .build();
        StudentEntity profile = StudentEntity.builder()
                .studentCode("SV001")
                .email("student@example.com")
                .phoneStudent("0901234567")
                .build();
        return UserEntity.builder()
                .userId("student-user-id")
                .userName("student01")
                .password("encoded-password")
                .status(StatusConstain.ACTIVE)
                .roles(Set.of(role))
                .student(profile)
                .build();
    }

    private UserEntity lecturerUser() {
        Roles role = Roles.builder().role(RoleConstain.SUPERVISOR).build();
        LectureEntity profile = LectureEntity.builder()
                .lectureCode("GV001")
                .emaillecture("lecturer@example.com")
                .phoneLecture("0912345678")
                .build();
        return UserEntity.builder()
                .userId("lecturer-user-id")
                .userName("lecturer01")
                .password("encoded-password")
                .status(StatusConstain.ACTIVE)
                .roles(Set.of(role))
                .lecture(profile)
                .build();
    }
}

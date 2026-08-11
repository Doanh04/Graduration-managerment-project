package com.graduration.Service.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.Validator;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.graduration.Constain.RoleConstain;
import com.graduration.Constain.StatusConstain;
import com.graduration.DTO.Request.RegisterLectureRequest;
import com.graduration.DTO.Request.UpdateLecturerRequest;
import com.graduration.DTO.Response.ImportLectureResponse;
import com.graduration.DTO.Response.RegisterLectureResponse;
import com.graduration.Repository.LectureRepository;
import com.graduration.Repository.RoleRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.LectureEntity;
import com.graduration.entity.Roles;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.UserMaper;

@ExtendWith(MockitoExtension.class)
class UserLecturerServiceTest {
    @Mock
    UserRepository userRepository;

    @Mock
    LectureRepository lectureRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    UserMaper userMaper;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    Validator validator;

    @Mock
    TransactionTemplate transactionTemplate;

    @InjectMocks
    UserLecturerService userLecturerService;

    Roles supervisorRole;

    @BeforeEach
    void setUp() {
        supervisorRole = Roles.builder().role(RoleConstain.SUPERVISOR).build();
        org.mockito.Mockito.lenient()
                .when(validator.validate(any(RegisterLectureRequest.class)))
                .thenReturn(Collections.emptySet());
    }

    @Test
    void registerLecturer_createsActiveUserAndLecturer() {
        RegisterLectureRequest request = validRequest();
        UserEntity user = new UserEntity();
        LectureEntity lecturer = new LectureEntity();
        RegisterLectureResponse expectedResponse = RegisterLectureResponse.builder()
                .userId("user-1")
                .userName(request.getUserName())
                .status(StatusConstain.ACTIVE.name())
                .roles(Set.of(RoleConstain.SUPERVISOR))
                .build();

        stubSuccessfulRegistration(request, user, lecturer, expectedResponse);

        RegisterLectureResponse actualResponse = userLecturerService.registerLecturer(request);

        assertSame(expectedResponse, actualResponse);
        assertEquals(StatusConstain.ACTIVE, user.getStatus());
        assertEquals("encoded-password", user.getPassword());
        assertEquals(Set.of(supervisorRole), user.getRoles());
        assertSame(user, lecturer.getUser());
        assertSame(lecturer, user.getLecture());
        verify(userRepository).save(user);
        verify(lectureRepository).save(lecturer);
    }

    @Test
    void registerLecturer_allowsMissingEmailAndPhone() {
        RegisterLectureRequest request = validRequest();
        request.setEmail("  ");
        request.setPhone("");

        UserEntity user = new UserEntity();
        LectureEntity lecturer = new LectureEntity();
        stubSuccessfulRegistration(request, user, lecturer, new RegisterLectureResponse());

        userLecturerService.registerLecturer(request);

        assertNull(request.getEmail());
        assertNull(request.getPhone());
        verify(lectureRepository, never()).existsByEmaillecture(any());
        verify(lectureRepository, never()).existsByPhoneLecture(any());
    }

    @Test
    void registerLecturer_rejectsDuplicateEmail() {
        RegisterLectureRequest request = validRequest();
        when(userRepository.existsByUserName(request.getUserName())).thenReturn(false);
        when(lectureRepository.existsByLectureCode(request.getLectureCode())).thenReturn(false);
        when(lectureRepository.existsByEmaillecture(request.getEmail())).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> userLecturerService.registerLecturer(request));

        assertEquals(ErrorCode.EMAIL_VERIFIED_EXITED, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(lectureRepository, never()).save(any());
    }

    @Test
    void registerLecturer_rejectsDuplicatePhone() {
        RegisterLectureRequest request = validRequest();
        when(userRepository.existsByUserName(request.getUserName())).thenReturn(false);
        when(lectureRepository.existsByLectureCode(request.getLectureCode())).thenReturn(false);
        when(lectureRepository.existsByEmaillecture(request.getEmail())).thenReturn(false);
        when(lectureRepository.existsByPhoneLecture(request.getPhone())).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> userLecturerService.registerLecturer(request));

        assertEquals(ErrorCode.PHONE_IS_EXITED, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteLecturerAccount_changesStatusToDeletedWithoutDeleting() {
        UserEntity user = UserEntity.builder()
                .userId("user-1")
                .status(StatusConstain.ACTIVE)
                .build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        userLecturerService.deleteLecturerAccount("user-1");

        assertEquals(StatusConstain.DELETED, user.getStatus());
        verify(userRepository).save(user);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void updateLecturer_preservesMissingFieldsAndDoesNotEncodeMissingPassword() {
        UpdateLecturerRequest request =
                UpdateLecturerRequest.builder().fullName("Updated Name").build();
        UserEntity user = UserEntity.builder()
                .userId("user-1")
                .userName("lecturer01")
                .password("existing-password")
                .status(StatusConstain.ACTIVE)
                .build();
        LectureEntity lecturer = LectureEntity.builder()
                .lectureId("lecturer-1")
                .lectureCode("GV001")
                .fullNameLecture("Old Name")
                .emaillecture("old@example.com")
                .build();
        RegisterLectureResponse expected = RegisterLectureResponse.builder()
                .userId("user-1")
                .fullName("Updated Name")
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(lectureRepository.findByUser_UserId("user-1")).thenReturn(Optional.of(lecturer));
        when(userRepository.save(user)).thenReturn(user);
        when(lectureRepository.save(lecturer)).thenReturn(lecturer);
        when(userMaper.toLectureResponse(user, lecturer)).thenReturn(expected);

        RegisterLectureResponse actual = userLecturerService.updateLecturer("user-1", request);

        assertSame(expected, actual);
        verify(userMaper).updateUserEntity(request, user);
        verify(userMaper).updateLecturerEntity(request, lecturer);
        verify(passwordEncoder, never()).encode(any());
        assertEquals("existing-password", user.getPassword());
    }

    @Test
    void updateLecturer_encodesPasswordWhenProvided() {
        UpdateLecturerRequest request =
                UpdateLecturerRequest.builder().password("newPassword123").build();
        UserEntity user =
                UserEntity.builder().userId("user-1").password("old-password").build();
        LectureEntity lecturer = LectureEntity.builder().lectureId("lecturer-1").build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(lectureRepository.findByUser_UserId("user-1")).thenReturn(Optional.of(lecturer));
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-encoded-password");
        when(userRepository.save(user)).thenReturn(user);
        when(lectureRepository.save(lecturer)).thenReturn(lecturer);

        userLecturerService.updateLecturer("user-1", request);

        assertEquals("new-encoded-password", user.getPassword());
        verify(passwordEncoder).encode("newPassword123");
    }

    @Test
    void updateLecturer_updatesRolesAndReturnsTheirPermissions() {
        UpdateLecturerRequest request = UpdateLecturerRequest.builder()
                .roles(Set.of(RoleConstain.SUPERVISOR, RoleConstain.REVIEWER))
                .build();
        UserEntity user = UserEntity.builder()
                .userId("user-1")
                .roles(Set.of(supervisorRole))
                .build();
        LectureEntity lecturer = LectureEntity.builder().lectureId("lecturer-1").build();
        Roles reviewerRole = Roles.builder().role(RoleConstain.REVIEWER).build();
        RegisterLectureResponse expected = RegisterLectureResponse.builder()
                .roles(Set.of(RoleConstain.SUPERVISOR, RoleConstain.REVIEWER))
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(lectureRepository.findByUser_UserId("user-1")).thenReturn(Optional.of(lecturer));
        when(roleRepository.findAllById(request.getRoles()))
                .thenReturn(java.util.List.of(supervisorRole, reviewerRole));
        when(userRepository.save(user)).thenReturn(user);
        when(lectureRepository.save(lecturer)).thenReturn(lecturer);
        when(userMaper.toLectureResponse(user, lecturer)).thenReturn(expected);

        RegisterLectureResponse response = userLecturerService.updateLecturer("user-1", request);

        assertEquals(Set.of(supervisorRole, reviewerRole), user.getRoles());
        assertEquals(Set.of(RoleConstain.SUPERVISOR, RoleConstain.REVIEWER), response.getRoles());
    }

    @Test
    void getAllLecturers_returnsMappedLecturerAccounts() {
        UserEntity firstUser =
                UserEntity.builder().userId("user-1").userName("lecturer01").build();
        UserEntity secondUser =
                UserEntity.builder().userId("user-2").userName("lecturer02").build();
        LectureEntity firstLecturer =
                LectureEntity.builder().lectureId("lecturer-1").user(firstUser).build();
        LectureEntity secondLecturer =
                LectureEntity.builder().lectureId("lecturer-2").user(secondUser).build();
        RegisterLectureResponse firstResponse =
                RegisterLectureResponse.builder().userName("lecturer01").build();
        RegisterLectureResponse secondResponse =
                RegisterLectureResponse.builder().userName("lecturer02").build();

        when(lectureRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(firstLecturer, secondLecturer)));
        when(userMaper.toLectureResponse(firstUser, firstLecturer)).thenReturn(firstResponse);
        when(userMaper.toLectureResponse(secondUser, secondLecturer)).thenReturn(secondResponse);

        java.util.List<RegisterLectureResponse> responses = userLecturerService.getAllLecturers();

        assertEquals(2, responses.size());
        assertEquals("lecturer01", responses.get(0).getUserName());
        assertEquals("lecturer02", responses.get(1).getUserName());
    }

    @Test
    void getLecturerByUserName_trimsUsernameAndReturnsMappedAccount() {
        UserEntity user =
                UserEntity.builder().userId("user-1").userName("lecturer01").build();
        LectureEntity lecturer =
                LectureEntity.builder().lectureId("lecturer-1").user(user).build();
        RegisterLectureResponse expected =
                RegisterLectureResponse.builder().userName("lecturer01").build();

        when(lectureRepository.findByUser_UserName("lecturer01")).thenReturn(Optional.of(lecturer));
        when(userMaper.toLectureResponse(user, lecturer)).thenReturn(expected);

        RegisterLectureResponse response = userLecturerService.getLecturerByUserName(" lecturer01 ");

        assertSame(expected, response);
        verify(lectureRepository).findByUser_UserName("lecturer01");
    }

    @Test
    void getLecturerByUserName_throwsWhenUserDoesNotExist() {
        when(lectureRepository.findByUser_UserName("missing")).thenReturn(Optional.empty());

        AppException exception =
                assertThrows(AppException.class, () -> userLecturerService.getLecturerByUserName("missing"));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void resetPasswordByUserName_encodesDefaultPasswordAndSavesUser() {
        UserEntity user = UserEntity.builder()
                .userId("user-1")
                .userName("lecturer01")
                .password("old-password")
                .build();
        LectureEntity lecturer = LectureEntity.builder().user(user).build();

        when(lectureRepository.findByUser_UserName("lecturer01")).thenReturn(Optional.of(lecturer));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-temporary-password");

        userLecturerService.resetPasswordByUserName(" lecturer01 ");

        assertEquals("encoded-temporary-password", user.getPassword());
        verify(passwordEncoder).encode(argThat(password -> password.length() == 16 && !"12345678".equals(password)));
        verify(userRepository).save(user);
    }

    @Test
    void resetPasswordByUserName_throwsWhenUserDoesNotExist() {
        when(lectureRepository.findByUser_UserName("missing")).thenReturn(Optional.empty());

        AppException exception =
                assertThrows(AppException.class, () -> userLecturerService.resetPasswordByUserName("missing"));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPasswordByUserName_rejectsBlankUsername() {
        AppException exception =
                assertThrows(AppException.class, () -> userLecturerService.resetPasswordByUserName("   "));

        assertEquals(ErrorCode.INVALID_USERNAME, exception.getErrorCode());
        verify(lectureRepository, never()).findByUser_UserName(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void importLecturers_readsXlsxAndReturnsSuccessfulResult() throws Exception {
        MockMultipartFile file = validExcelFile();
        RegisterLectureResponse importedResponse = RegisterLectureResponse.builder()
                .userId("user-1")
                .userName("lecturer01")
                .build();

        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<RegisterLectureResponse> callback = invocation.getArgument(0);
            return callback.doInTransaction(org.mockito.Mockito.mock(TransactionStatus.class));
        });
        when(userMaper.toUserEntity(any(RegisterLectureRequest.class))).thenReturn(new UserEntity());
        when(userMaper.toLecturerEntity(any(RegisterLectureRequest.class))).thenReturn(new LectureEntity());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(roleRepository.findById(RoleConstain.SUPERVISOR)).thenReturn(Optional.of(supervisorRole));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lectureRepository.save(any(LectureEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMaper.toLectureResponse(any(UserEntity.class), any(LectureEntity.class)))
                .thenReturn(importedResponse);

        ImportLectureResponse response = userLecturerService.importLecturers(file);

        assertEquals(1, response.getTotalRows());
        assertEquals(1, response.getSuccessRows());
        assertEquals(0, response.getFailedRows());
        assertEquals("lecturer01", response.getImportedLecturers().get(0).getUserName());

        ArgumentCaptor<RegisterLectureRequest> requestCaptor = ArgumentCaptor.forClass(RegisterLectureRequest.class);
        verify(userMaper).toUserEntity(requestCaptor.capture());
        assertEquals("GV001", requestCaptor.getValue().getLectureCode());
        assertNull(requestCaptor.getValue().getEmail());
    }

    @Test
    void importLecturers_rejectsNonXlsxFile() {
        MockMultipartFile file = new MockMultipartFile("file", "lecturers.csv", "text/csv", "invalid".getBytes());

        AppException exception = assertThrows(AppException.class, () -> userLecturerService.importLecturers(file));

        assertEquals(ErrorCode.INVALID_EXCEL_FILE, exception.getErrorCode());
    }

    private void stubSuccessfulRegistration(
            RegisterLectureRequest request, UserEntity user, LectureEntity lecturer, RegisterLectureResponse response) {
        when(userMaper.toUserEntity(request)).thenReturn(user);
        when(userMaper.toLecturerEntity(request)).thenReturn(lecturer);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(roleRepository.findById(RoleConstain.SUPERVISOR)).thenReturn(Optional.of(supervisorRole));
        when(userRepository.save(user)).thenReturn(user);
        when(lectureRepository.save(lecturer)).thenReturn(lecturer);
        when(userMaper.toLectureResponse(user, lecturer)).thenReturn(response);
    }

    private RegisterLectureRequest validRequest() {
        return RegisterLectureRequest.builder()
                .userName("lecturer01")
                .password("password123")
                .lectureCode("GV001")
                .fullName("Nguyen Van A")
                .degree("Master")
                .email("lecturer@example.com")
                .phone("0901234567")
                .build();
    }

    private MockMultipartFile validExcelFile() throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Lecturers");
            Row header = sheet.createRow(0);
            String[] headers = {"userName", "password", "lectureCode", "fullName", "degree", "email", "phone"};
            for (int index = 0; index < headers.length; index++) {
                header.createCell(index).setCellValue(headers[index]);
            }

            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("lecturer01");
            data.createCell(1).setCellValue("password123");
            data.createCell(2).setCellValue("GV001");
            data.createCell(3).setCellValue("Nguyen Van A");
            data.createCell(4).setCellValue("Master");
            data.createCell(5).setCellValue("");
            data.createCell(6).setCellValue("0901234567");

            workbook.write(output);
            return new MockMultipartFile(
                    "file",
                    "lecturers.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray());
        }
    }
}

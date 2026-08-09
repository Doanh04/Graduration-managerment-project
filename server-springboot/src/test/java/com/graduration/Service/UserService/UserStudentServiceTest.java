package com.graduration.Service.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.graduration.DTO.Request.RegisterStudentRequest;
import com.graduration.DTO.Response.RegisterStudentResponse;
import com.graduration.Repository.ClassRepository;
import com.graduration.Repository.RoleRepository;
import com.graduration.Repository.StudentRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.Service.UserService.UserStudentService.ImportStudentResult;
import com.graduration.entity.ClassEntity;
import com.graduration.entity.Roles;
import com.graduration.entity.StudentEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.UserMaper;

@ExtendWith(MockitoExtension.class)
class UserStudentServiceTest {
    @Mock
    UserRepository userRepository;

    @Mock
    StudentRepository studentRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    ClassRepository classRepository;

    @Mock
    UserMaper userMaper;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    TransactionTemplate transactionTemplate;

    @InjectMocks
    UserStudentService userStudentService;

    Roles studentRole;
    ClassEntity studentClass;

    @BeforeEach
    void setUp() {
        studentRole = Roles.builder().role(RoleConstain.STUDENT).build();
        studentClass = ClassEntity.builder().classId(10L).classCode("CNTT01").build();
    }

    @Test
    void registerStudent_createsLinkedActiveUserAndStudent() {
        RegisterStudentRequest request = validRequest();
        UserEntity user = new UserEntity();
        StudentEntity student = new StudentEntity();
        RegisterStudentResponse expected = RegisterStudentResponse.builder()
                .idUser("user-1")
                .userName("student01")
                .build();

        stubSuccessfulRegistration(request, user, student, expected);

        RegisterStudentResponse response = userStudentService.registerStudent(request);

        assertSame(expected, response);
        assertEquals("encoded-password", user.getPassword());
        assertEquals(StatusConstain.ACTIVE, user.getStatus());
        assertNotNull(user.getCreateAt());
        assertEquals(Set.of(studentRole), user.getRoles());
        assertSame(user, student.getUserEntity());
        assertSame(studentClass, student.getClassEntity());
        assertSame(student, user.getStudent());
        verify(userRepository).save(user);
        verify(studentRepository).save(student);
    }

    @Test
    void registerStudent_rejectsDuplicateUsername() {
        RegisterStudentRequest request = validRequest();
        when(userRepository.existsByUserName("student01")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> userStudentService.registerStudent(request));

        assertEquals(ErrorCode.USERNAME_IS_EXITED, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void getStudentByUserName_trimsUserNameAndReturnsMappedStudent() {
        StudentEntity student = StudentEntity.builder().studentCode("SV001").build();
        UserEntity user = UserEntity.builder()
                .userId("user-1")
                .userName("student01")
                .student(student)
                .build();
        RegisterStudentResponse expected =
                RegisterStudentResponse.builder().idUser("user-1").build();
        when(userRepository.findByUserName("student01")).thenReturn(Optional.of(user));
        when(userMaper.toStudentResponse(user, student)).thenReturn(expected);

        RegisterStudentResponse response = userStudentService.getStudentByUserName(" student01 ");

        assertSame(expected, response);
        verify(userRepository).findByUserName("student01");
    }

    @Test
    void getStudentByUserName_rejectsUserWithoutStudentProfile() {
        UserEntity user =
                UserEntity.builder().userId("user-1").userName("student01").build();
        when(userRepository.findByUserName("student01")).thenReturn(Optional.of(user));

        AppException exception =
                assertThrows(AppException.class, () -> userStudentService.getStudentByUserName("student01"));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getAllStudents_returnsMappedAccounts() {
        UserEntity firstUser = UserEntity.builder().userId("user-1").build();
        UserEntity secondUser = UserEntity.builder().userId("user-2").build();
        StudentEntity firstStudent = StudentEntity.builder()
                .studentCode("SV001")
                .userEntity(firstUser)
                .build();
        StudentEntity secondStudent = StudentEntity.builder()
                .studentCode("SV002")
                .userEntity(secondUser)
                .build();
        RegisterStudentResponse firstResponse =
                RegisterStudentResponse.builder().studentCode("SV001").build();
        RegisterStudentResponse secondResponse =
                RegisterStudentResponse.builder().studentCode("SV002").build();
        when(studentRepository.findAll()).thenReturn(List.of(firstStudent, secondStudent));
        when(userMaper.toStudentResponse(firstUser, firstStudent)).thenReturn(firstResponse);
        when(userMaper.toStudentResponse(secondUser, secondStudent)).thenReturn(secondResponse);

        List<RegisterStudentResponse> responses = userStudentService.getAllStudents();

        assertEquals(2, responses.size());
        assertEquals("SV001", responses.get(0).getStudentCode());
        assertEquals("SV002", responses.get(1).getStudentCode());
    }

    @Test
    void resetPasswordByUserName_encodesDefaultPasswordAndSavesUser() {
        StudentEntity student = new StudentEntity();
        UserEntity user = UserEntity.builder()
                .userId("user-1")
                .userName("student01")
                .password("old-password")
                .student(student)
                .build();
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(passwordEncoder.encode("12345678")).thenReturn("encoded-default-password");

        userStudentService.resetPasswordByUserName(" student01 ");

        assertEquals("encoded-default-password", user.getPassword());
        verify(passwordEncoder).encode("12345678");
        verify(userRepository).save(user);
    }

    @Test
    void deleteStudentAccount_byUserNameSetsDeletedStatusWithoutPhysicalDelete() {
        StudentEntity student = new StudentEntity();
        UserEntity user = UserEntity.builder()
                .userId("user-1")
                .userName("student01")
                .status(StatusConstain.ACTIVE)
                .student(student)
                .build();
        when(userRepository.findByUserName("student01")).thenReturn(Optional.of(user));

        userStudentService.deleteStudentAccount(" student01 ");

        assertEquals(StatusConstain.DELETED, user.getStatus());
        verify(userRepository).findByUserName("student01");
        verify(userRepository).save(user);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void importStudents_readsXlsxAndReturnsSuccessfulResult() throws Exception {
        MockMultipartFile file = validExcelFile();
        RegisterStudentResponse imported = RegisterStudentResponse.builder()
                .idUser("user-1")
                .userName("student01")
                .studentCode("SV001")
                .build();

        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<RegisterStudentResponse> callback = invocation.getArgument(0);
            return callback.doInTransaction(org.mockito.Mockito.mock(TransactionStatus.class));
        });
        when(userMaper.toUserEntity(any(RegisterStudentRequest.class))).thenReturn(new UserEntity());
        when(userMaper.toStudentEntity(any(RegisterStudentRequest.class))).thenReturn(new StudentEntity());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(roleRepository.findById(RoleConstain.STUDENT)).thenReturn(Optional.of(studentRole));
        when(classRepository.findById(10L)).thenReturn(Optional.of(studentClass));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.save(any(StudentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMaper.toStudentResponse(any(UserEntity.class), any(StudentEntity.class)))
                .thenReturn(imported);

        ImportStudentResult result = userStudentService.importStudents(file);

        assertEquals(1, result.totalRows());
        assertEquals(1, result.successRows());
        assertEquals(0, result.failedRows());
        assertEquals("SV001", result.importedStudents().get(0).getStudentCode());
    }

    @Test
    void importStudents_rejectsNonXlsxFile() {
        MockMultipartFile file = new MockMultipartFile("file", "students.csv", "text/csv", "invalid".getBytes());

        AppException exception = assertThrows(AppException.class, () -> userStudentService.importStudents(file));

        assertEquals(ErrorCode.INVALID_EXCEL_FILE, exception.getErrorCode());
        verify(transactionTemplate, never()).execute(any(TransactionCallback.class));
    }

    private void stubSuccessfulRegistration(
            RegisterStudentRequest request, UserEntity user, StudentEntity student, RegisterStudentResponse response) {
        when(userRepository.existsByUserName("student01")).thenReturn(false);
        when(studentRepository.findAll()).thenReturn(List.of());
        when(roleRepository.findById(RoleConstain.STUDENT)).thenReturn(Optional.of(studentRole));
        when(classRepository.findById(10L)).thenReturn(Optional.of(studentClass));
        when(userMaper.toUserEntity(request)).thenReturn(user);
        when(userMaper.toStudentEntity(request)).thenReturn(student);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(user)).thenReturn(user);
        when(studentRepository.save(student)).thenReturn(student);
        when(userMaper.toStudentResponse(user, student)).thenReturn(response);
    }

    private RegisterStudentRequest validRequest() {
        return RegisterStudentRequest.builder()
                .userName("student01")
                .password("password123")
                .studentCode("SV001")
                .fullName("Nguyen Van A")
                .email("student@example.com")
                .phone("0901234567")
                .classId(10L)
                .build();
    }

    private MockMultipartFile validExcelFile() throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("students");
            Row header = sheet.createRow(0);
            String[] headers = {"userName", "password", "studentCode", "fullName", "email", "phone", "classId"};
            for (int column = 0; column < headers.length; column++) {
                header.createCell(column).setCellValue(headers[column]);
            }

            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("student01");
            data.createCell(1).setCellValue("password123");
            data.createCell(2).setCellValue("SV001");
            data.createCell(3).setCellValue("Nguyen Van A");
            data.createCell(4).setCellValue("");
            data.createCell(5).setCellValue("0901234567");
            data.createCell(6).setCellValue(10);

            workbook.write(output);
            return new MockMultipartFile(
                    "file",
                    "students.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray());
        }
    }
}

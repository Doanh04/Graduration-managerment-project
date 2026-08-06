package com.graduration.Service.UserService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.graduration.Constain.RoleConstain;
import com.graduration.Constain.StatusConstain;
import com.graduration.DTO.Request.RegisterStudentRequest;
import com.graduration.DTO.Response.RegisterStudentResponse;
import com.graduration.Repository.ClassRepository;
import com.graduration.Repository.RoleRepository;
import com.graduration.Repository.StudentRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.ClassEntity;
import com.graduration.entity.Roles;
import com.graduration.entity.StudentEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.StudentMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserStudentService {
    private static final String DEFAULT_PASSWORD = "12345678";

    UserRepository userRepository;
    StudentRepository studentRepository;
    RoleRepository roleRepository;
    ClassRepository classRepository;
    StudentMapper studentMapper;
    PasswordEncoder passwordEncoder;
    TransactionTemplate transactionTemplate;

    @Transactional
    public RegisterStudentResponse registerStudent(RegisterStudentRequest request) {
        normalizeRequest(request);
        validateRequest(request);
        validateUniqueness(request);

        Roles studentRole = roleRepository
                .findById(RoleConstain.STUDENT)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        ClassEntity studentClass = classRepository
                .findById(request.getClassId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));

        UserEntity user = studentMapper.toUserEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(StatusConstain.ACTIVE);
        user.setCreateAt(LocalDateTime.now());
        user.setRoles(new HashSet<>(Set.of(studentRole)));
        user = userRepository.save(user);

        StudentEntity student = studentMapper.toStudentEntity(request);
        student.setUserEntity(user);
        student.setClassEntity(studentClass);
        student = studentRepository.save(student);

        user.setStudent(student);
        return studentMapper.toStudentResponse(user, student);
    }

    @Transactional(readOnly = true)
    public RegisterStudentResponse getStudentByUserName(String userName) {
        if (userName == null || userName.isBlank()) {
            throw new AppException(ErrorCode.INVALID_USERNAME);
        }

        UserEntity user = userRepository
                .findByUserName(userName.trim())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        StudentEntity student = user.getStudent();
        if (student == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        return studentMapper.toStudentResponse(user, student);
    }

    @Transactional(readOnly = true)
    public List<RegisterStudentResponse> getAllStudents() {
        return studentRepository.findAll().stream()
                .map(student -> studentMapper.toStudentResponse(student.getUserEntity(), student))
                .toList();
    }

    @Transactional
    public void resetPasswordByUserName(String userName) {
        if (userName == null || userName.isBlank()) {
            throw new AppException(ErrorCode.INVALID_USERNAME);
        }

        UserEntity user = userRepository.findAll().stream()
                .filter(candidate -> userName.trim().equals(candidate.getUserName()))
                .filter(candidate -> candidate.getStudent() != null)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        userRepository.save(user);
    }

    @Transactional
    public void deleteStudentAccount(String userName) {
        if (userName == null || userName.isBlank()) {
            throw new AppException(ErrorCode.INVALID_USERNAME);
        }

        UserEntity user = userRepository
                .findByUserName(userName.trim())
                .filter(candidate -> candidate.getStudent() != null)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setStatus(StatusConstain.DELETED);
        userRepository.save(user);
    }

    public ImportStudentResult importStudents(MultipartFile file) {
        validateExcelFile(file);

        List<RegisterStudentResponse> importedStudents = new ArrayList<>();
        List<ImportStudentError> errors = new ArrayList<>();
        int totalRows = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            validateExcelHeader(sheet.getRow(0), formatter);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isEmptyRow(row, formatter)) {
                    continue;
                }

                totalRows++;
                RegisterStudentRequest request;
                try {
                    request = readRequest(row, formatter);
                    RegisterStudentResponse response = transactionTemplate.execute(status -> registerStudent(request));
                    importedStudents.add(response);
                } catch (RuntimeException exception) {
                    errors.add(
                            new ImportStudentError(rowIndex + 1, cellValue(row, 0, formatter), exception.getMessage()));
                }
            }
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof AppException appException) {
                throw appException;
            }
            throw new AppException(ErrorCode.INVALID_EXCEL_FILE);
        }

        return new ImportStudentResult(totalRows, importedStudents.size(), errors.size(), importedStudents, errors);
    }

    private void validateRequest(RegisterStudentRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        if (request.getUserName() == null || request.getUserName().isBlank()) {
            throw new AppException(ErrorCode.USERNAME_NOT_BLANK);
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new AppException(ErrorCode.PASSWORD_NOT_BLANK);
        }
        if (request.getStudentCode() == null || request.getStudentCode().isBlank()) {
            throw new AppException(ErrorCode.STUDENT_NOT_BLANK);
        }
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new AppException(ErrorCode.FULLNAME_NOT_BLANK);
        }
        if (request.getClassId() == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }

    private void validateUniqueness(RegisterStudentRequest request) {
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new AppException(ErrorCode.USERNAME_IS_EXITED);
        }

        studentRepository.findAll().forEach(student -> {
            if (request.getStudentCode().equals(student.getStudentCode())) {
                throw new AppException(ErrorCode.USER_EXITED);
            }
            if (request.getEmail() != null && request.getEmail().equalsIgnoreCase(student.getEmail())) {
                throw new AppException(ErrorCode.EMAIL_VERIFIED_EXITED);
            }
            if (request.getPhone() != null && request.getPhone().equals(student.getPhoneStudent())) {
                throw new AppException(ErrorCode.PHONE_IS_EXITED);
            }
        });
    }

    private void normalizeRequest(RegisterStudentRequest request) {
        if (request == null) {
            return;
        }
        request.setUserName(trim(request.getUserName()));
        request.setStudentCode(trim(request.getStudentCode()));
        request.setFullName(trim(request.getFullName()));
        request.setEmail(normalize(request.getEmail()));
        request.setPhone(normalize(request.getPhone()));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateExcelFile(MultipartFile file) {
        if (file == null
                || file.isEmpty()
                || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new AppException(ErrorCode.INVALID_EXCEL_FILE);
        }
    }

    private void validateExcelHeader(Row header, DataFormatter formatter) {
        String[] expectedHeaders = {"userName", "password", "studentCode", "fullName", "email", "phone", "classId"};

        if (header == null) {
            throw new AppException(ErrorCode.INVALID_EXCEL_FILE);
        }

        for (int column = 0; column < expectedHeaders.length; column++) {
            String actualHeader =
                    formatter.formatCellValue(header.getCell(column)).trim();
            if (!expectedHeaders[column].equalsIgnoreCase(actualHeader)) {
                throw new AppException(ErrorCode.INVALID_EXCEL_FILE);
            }
        }
    }

    private RegisterStudentRequest readRequest(Row row, DataFormatter formatter) {
        String classId = cellValue(row, 6, formatter);
        if (classId == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        try {
            return RegisterStudentRequest.builder()
                    .userName(cellValue(row, 0, formatter))
                    .password(cellValue(row, 1, formatter))
                    .studentCode(cellValue(row, 2, formatter))
                    .fullName(cellValue(row, 3, formatter))
                    .email(cellValue(row, 4, formatter))
                    .phone(cellValue(row, 5, formatter))
                    .classId(Long.valueOf(classId))
                    .build();
        } catch (NumberFormatException exception) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }

    private String cellValue(Row row, int column, DataFormatter formatter) {
        return normalize(formatter.formatCellValue(row.getCell(column)));
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter) {
        for (int column = 0; column <= 6; column++) {
            if (!formatter.formatCellValue(row.getCell(column)).isBlank()) {
                return false;
            }
        }
        return true;
    }

    public record ImportStudentResult(
            int totalRows,
            int successRows,
            int failedRows,
            List<RegisterStudentResponse> importedStudents,
            List<ImportStudentError> errors) {}

    public record ImportStudentError(int row, String userName, String message) {}
}

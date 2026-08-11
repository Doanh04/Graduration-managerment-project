package com.graduration.Service.UserService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Configuration.TemporaryPasswordGenerator;
import com.graduration.Constain.RoleConstain;
import com.graduration.Constain.StatusConstain;
import com.graduration.DTO.Request.RegisterLectureRequest;
import com.graduration.DTO.Request.UpdateLecturerRequest;
import com.graduration.DTO.Response.ImportLectureErrorResponse;
import com.graduration.DTO.Response.ImportLectureResponse;
import com.graduration.DTO.Response.PasswordResetResponse;
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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserLecturerService {
    UserRepository userRepository;
    LectureRepository lectureRepository;
    RoleRepository roleRepository;
    UserMaper userMaper;
    PasswordEncoder passwordEncoder;
    Validator validator;
    TransactionTemplate transactionTemplate;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @Transactional
    public RegisterLectureResponse registerLecturer(RegisterLectureRequest request) {
        normalizeRequest(request);
        validateRequest(request);
        validateUniqueness(request);

        Roles lecturerRole = roleRepository
                .findById(RoleConstain.SUPERVISOR)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        UserEntity user = userMaper.toUserEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(StatusConstain.ACTIVE);
        user.setCreateAt(LocalDateTime.now());
        user.setRoles(new HashSet<>(Set.of(lecturerRole)));
        user = userRepository.save(user);

        LectureEntity lecturer = userMaper.toLecturerEntity(request);
        lecturer.setUser(user);
        lecturer = lectureRepository.save(lecturer);

        user.setLecture(lecturer);
        return userMaper.toLectureResponse(user, lecturer);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @Transactional
    public void deleteLecturerAccount(String userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setStatus(StatusConstain.DELETED);
        userRepository.save(user);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @Transactional
    public RegisterLectureResponse updateLecturer(String userId, UpdateLecturerRequest request) {
        normalizeUpdateRequest(request);
        validateUpdateRequest(request);

        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        LectureEntity lecturer = lectureRepository
                .findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        validateUpdateUniqueness(userId, lecturer.getLectureId(), request);
        userMaper.updateUserEntity(request, user);
        userMaper.updateLecturerEntity(request, lecturer);

        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRoles() != null) {
            List<Roles> roles = roleRepository.findAllById(request.getRoles());
            if (roles.size() != request.getRoles().size()) {
                throw new AppException(ErrorCode.ROLE_NOT_FOUND);
            }
            user.setRoles(new HashSet<>(roles));
        }

        user = userRepository.save(user);
        lecturer = lectureRepository.save(lecturer);
        return userMaper.toLectureResponse(user, lecturer);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public List<RegisterLectureResponse> getAllLecturers() {
        return getAllLecturers(0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public List<RegisterLectureResponse> getAllLecturers(Integer page, Integer size) {
        return lectureRepository.findAll(PaginationSupport.pageRequest(page, size)).stream()
                .map(lecturer -> userMaper.toLectureResponse(lecturer.getUser(), lecturer))
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public RegisterLectureResponse getLecturerByUserName(String userName) {
        if (userName == null || userName.isBlank()) {
            throw new AppException(ErrorCode.INVALID_USERNAME);
        }

        LectureEntity lecturer = lectureRepository
                .findByUser_UserName(userName.trim())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return userMaper.toLectureResponse(lecturer.getUser(), lecturer);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @Transactional
    public PasswordResetResponse resetPasswordByUserName(String userName) {
        if (userName == null || userName.isBlank()) {
            throw new AppException(ErrorCode.INVALID_USERNAME);
        }

        UserEntity user = lectureRepository
                .findByUser_UserName(userName.trim())
                .map(LectureEntity::getUser)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String temporaryPassword = TemporaryPasswordGenerator.generate();
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        userRepository.save(user);
        return PasswordResetResponse.builder()
                .userName(user.getUserName())
                .temporaryPassword(temporaryPassword)
                .build();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ImportLectureResponse importLecturers(MultipartFile file) {
        validateExcelFile(file);

        List<RegisterLectureResponse> importedLecturers = new ArrayList<>();
        List<ImportLectureErrorResponse> errors = new ArrayList<>();
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
                RegisterLectureRequest request = readRequest(row, formatter);

                try {
                    RegisterLectureResponse response = transactionTemplate.execute(status -> registerLecturer(request));
                    importedLecturers.add(response);
                } catch (RuntimeException exception) {
                    errors.add(ImportLectureErrorResponse.builder()
                            .row(rowIndex + 1)
                            .userName(request.getUserName())
                            .message(exception.getMessage())
                            .build());
                }
            }
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof AppException appException) {
                throw appException;
            }
            throw new AppException(ErrorCode.INVALID_EXCEL_FILE);
        }

        return ImportLectureResponse.builder()
                .totalRows(totalRows)
                .successRows(importedLecturers.size())
                .failedRows(errors.size())
                .importedLecturers(importedLecturers)
                .errors(errors)
                .build();
    }

    private void validateRequest(RegisterLectureRequest request) {
        Set<ConstraintViolation<RegisterLectureRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String errorKey = violations.iterator().next().getMessage();
            try {
                throw new AppException(ErrorCode.valueOf(errorKey));
            } catch (IllegalArgumentException exception) {
                throw new AppException(ErrorCode.EXCEL_ROW_INVALID);
            }
        }
    }

    private void validateUniqueness(RegisterLectureRequest request) {
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new AppException(ErrorCode.USERNAME_IS_EXITED);
        }
        if (lectureRepository.existsByLectureCode(request.getLectureCode())) {
            throw new AppException(ErrorCode.LECTURER_CODE_IS_EXITED);
        }
        if (request.getEmail() != null && lectureRepository.existsByEmaillecture(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_VERIFIED_EXITED);
        }
        if (request.getPhone() != null && lectureRepository.existsByPhoneLecture(request.getPhone())) {
            throw new AppException(ErrorCode.PHONE_IS_EXITED);
        }
    }

    private void validateUpdateRequest(UpdateLecturerRequest request) {
        Set<ConstraintViolation<UpdateLecturerRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String errorKey = violations.iterator().next().getMessage();
            try {
                throw new AppException(ErrorCode.valueOf(errorKey));
            } catch (IllegalArgumentException exception) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }
        }
    }

    private void validateUpdateUniqueness(String userId, String lecturerId, UpdateLecturerRequest request) {
        if (request.getUserName() != null
                && userRepository.existsByUserNameAndUserIdNot(request.getUserName(), userId)) {
            throw new AppException(ErrorCode.USERNAME_IS_EXITED);
        }
        if (request.getLectureCode() != null
                && lectureRepository.existsByLectureCodeAndLectureIdNot(request.getLectureCode(), lecturerId)) {
            throw new AppException(ErrorCode.LECTURER_CODE_IS_EXITED);
        }
        if (request.getEmail() != null
                && lectureRepository.existsByEmaillectureAndLectureIdNot(request.getEmail(), lecturerId)) {
            throw new AppException(ErrorCode.EMAIL_VERIFIED_EXITED);
        }
        if (request.getPhone() != null
                && lectureRepository.existsByPhoneLectureAndLectureIdNot(request.getPhone(), lecturerId)) {
            throw new AppException(ErrorCode.PHONE_IS_EXITED);
        }
    }

    private void normalizeRequest(RegisterLectureRequest request) {
        request.setUserName(trim(request.getUserName()));
        request.setLectureCode(trim(request.getLectureCode()));
        request.setFullName(trim(request.getFullName()));
        request.setDegree(normalize(request.getDegree()));
        request.setEmail(normalize(request.getEmail()));
        request.setPhone(normalize(request.getPhone()));
    }

    private void normalizeUpdateRequest(UpdateLecturerRequest request) {
        request.setUserName(trim(request.getUserName()));
        request.setLectureCode(trim(request.getLectureCode()));
        request.setFullName(trim(request.getFullName()));
        request.setDegree(normalize(request.getDegree()));
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
        String[] expectedHeaders = {"userName", "password", "lectureCode", "fullName", "degree", "email", "phone"};

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

    private RegisterLectureRequest readRequest(Row row, DataFormatter formatter) {
        return RegisterLectureRequest.builder()
                .userName(cellValue(row, 0, formatter))
                .password(cellValue(row, 1, formatter))
                .lectureCode(cellValue(row, 2, formatter))
                .fullName(cellValue(row, 3, formatter))
                .degree(cellValue(row, 4, formatter))
                .email(cellValue(row, 5, formatter))
                .phone(cellValue(row, 6, formatter))
                .build();
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
}

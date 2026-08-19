package com.graduration.Service.UserService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
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
import com.graduration.DTO.Request.RegisterStudentRequest;
import com.graduration.DTO.Request.UpdateStudentRequest;
import com.graduration.DTO.Response.PasswordResetResponse;
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
import com.graduration.mapper.UserMaper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserStudentService {
    UserRepository userRepository;
    StudentRepository studentRepository;
    RoleRepository roleRepository;
    ClassRepository classRepository;
    UserMaper userMaper;
    PasswordEncoder passwordEncoder;
    TransactionTemplate transactionTemplate;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
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

        UserEntity user = userMaper.toUserEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(StatusConstain.ACTIVE);
        user.setCreateAt(LocalDateTime.now());
        user.setRoles(new HashSet<>(Set.of(studentRole)));
        user = userRepository.save(user);

        StudentEntity student = userMaper.toStudentEntity(request);
        student.setUserEntity(user);
        student.setClassEntity(studentClass);
        student = studentRepository.save(student);

        user.setStudent(student);
        return userMaper.toStudentResponse(user, student);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
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

        return userMaper.toStudentResponse(user, student);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public List<RegisterStudentResponse> getAllStudents() {
        return getAllStudents(0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public List<RegisterStudentResponse> getAllStudents(Integer page, Integer size) {
        return studentRepository.findAll(PaginationSupport.pageRequest(page, size)).stream()
                .map(student -> userMaper.toStudentResponse(student.getUserEntity(), student))
                .toList();
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public com.graduration.DTO.Response.PageResponse<RegisterStudentResponse> getAllStudentsPage(
            Integer page, Integer size) {
        return getAllStudentsPage(page, size, null);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public com.graduration.DTO.Response.PageResponse<RegisterStudentResponse> getAllStudentsPage(
            Integer page, Integer size, String keyword) {
        var pageable = PaginationSupport.pageRequest(page, size);
        var students = keyword == null || keyword.isBlank()
                ? studentRepository.findAll(pageable)
                : studentRepository.searchByNameOrCode(keyword.trim(), pageable);
        return com.graduration.DTO.Response.PageResponse.from(
                students, student -> userMaper.toStudentResponse(student.getUserEntity(), student));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @Transactional
    public PasswordResetResponse resetPasswordByUserName(String userName) {
        if (userName == null || userName.isBlank()) {
            throw new AppException(ErrorCode.INVALID_USERNAME);
        }

        UserEntity user = userRepository.findAll().stream()
                .filter(candidate -> userName.trim().equals(candidate.getUserName()))
                .filter(candidate -> candidate.getStudent() != null)
                .findFirst()
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
    @Transactional
    public RegisterStudentResponse updateStudent(String userId, UpdateStudentRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        UserEntity user = userRepository
                .findById(userId)
                .filter(candidate -> candidate.getStudent() != null)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        StudentEntity student = user.getStudent();

        String userName = trim(request.getUserName());
        String studentCode = trim(request.getStudentCode());
        String fullName = trim(request.getFullName());
        String email = normalize(request.getEmail());
        String phone = normalize(request.getPhone());
        if (userName == null || userName.isBlank()) throw new AppException(ErrorCode.USERNAME_NOT_BLANK);
        if (studentCode == null || studentCode.isBlank()) throw new AppException(ErrorCode.STUDENT_NOT_BLANK);
        if (fullName == null || fullName.isBlank()) throw new AppException(ErrorCode.FULLNAME_NOT_BLANK);
        if (request.getClassId() == null) throw new AppException(ErrorCode.INVALID_KEY);

        if (userRepository.existsByUserNameAndUserIdNot(userName, userId))
            throw new AppException(ErrorCode.USERNAME_IS_EXITED);
        if (studentRepository.existsByStudentCodeIgnoreCaseAndIdStudentNot(studentCode, student.getIdStudent()))
            throw new AppException(ErrorCode.USER_EXITED);
        if (email != null && studentRepository.existsByEmailIgnoreCaseAndIdStudentNot(email, student.getIdStudent()))
            throw new AppException(ErrorCode.EMAIL_VERIFIED_EXITED);
        if (phone != null && studentRepository.existsByPhoneStudentAndIdStudentNot(phone, student.getIdStudent()))
            throw new AppException(ErrorCode.PHONE_IS_EXITED);

        ClassEntity studentClass = classRepository
                .findById(request.getClassId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
        user.setUserName(userName);
        student.setStudentCode(studentCode);
        student.setFullNameStudent(fullName);
        student.setEmail(email);
        student.setPhoneStudent(phone);
        student.setClassEntity(studentClass);
        userRepository.save(user);
        studentRepository.save(student);
        return userMaper.toStudentResponse(user, student);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
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

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ImportStudentResult importStudents(MultipartFile file) {
        validateExcelFile(file);

        List<RegisterStudentResponse> importedStudents = new ArrayList<>();
        List<ImportStudentError> errors = new ArrayList<>();
        List<PendingStudentImport> pendingImports = new ArrayList<>();
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
                try {
                    pendingImports.add(new PendingStudentImport(rowIndex + 1, readRequest(row, formatter)));
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

        ensureStudentsDoNotExist(pendingImports);
        for (PendingStudentImport pending : pendingImports) {
            try {
                RegisterStudentResponse response =
                        transactionTemplate.execute(status -> registerStudent(pending.request()));
                importedStudents.add(response);
            } catch (RuntimeException exception) {
                errors.add(
                        new ImportStudentError(pending.row(), pending.request().getUserName(), exception.getMessage()));
            }
        }

        return new ImportStudentResult(totalRows, importedStudents.size(), errors.size(), importedStudents, errors);
    }

    private void ensureStudentsDoNotExist(List<PendingStudentImport> pendingImports) {
        Set<String> userNames = new HashSet<>();
        Set<String> studentCodes = new HashSet<>();
        Set<String> emails = new HashSet<>();
        Set<String> phones = new HashSet<>();
        for (PendingStudentImport pending : pendingImports) {
            RegisterStudentRequest request = pending.request();
            normalizeRequest(request);
            if (isDuplicate(userNames, request.getUserName())
                    || isDuplicate(studentCodes, request.getStudentCode())
                    || isDuplicate(emails, request.getEmail())
                    || isDuplicate(phones, request.getPhone())
                    || userRepository.existsByUserName(request.getUserName())
                    || studentRepository.existsByStudentCodeIgnoreCase(request.getStudentCode())
                    || (request.getEmail() != null && studentRepository.existsByEmailIgnoreCase(request.getEmail()))
                    || (request.getPhone() != null && studentRepository.existsByPhoneStudent(request.getPhone()))) {
                throw new AppException(ErrorCode.IMPORT_DATA_ALREADY_EXISTS);
            }
        }
    }

    private boolean isDuplicate(Set<String> values, String value) {
        return value != null && !values.add(value.toLowerCase(Locale.ROOT));
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

        if (studentRepository.existsByStudentCodeIgnoreCase(request.getStudentCode())) {
            throw new AppException(ErrorCode.USER_EXITED);
        }
        if (request.getEmail() != null && studentRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_VERIFIED_EXITED);
        }
        if (request.getPhone() != null && studentRepository.existsByPhoneStudent(request.getPhone())) {
            throw new AppException(ErrorCode.PHONE_IS_EXITED);
        }
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

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public byte[] exportStudentsByCreationYear(Integer year) {
        if (year == null || year < 2000 || year > 2100) throw new AppException(ErrorCode.INVALID_KEY);
        List<StudentEntity> students = studentRepository.findForExportByCreatedAt(
                LocalDateTime.of(year, 1, 1, 0, 0), LocalDateTime.of(year + 1, 1, 1, 0, 0));

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Danh sách sinh viên");
            int[] widths = {8, 18, 30, 18, 30, 16};
            for (int index = 0; index < widths.length; index++) sheet.setColumnWidth(index, widths[index] * 256);

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldFont.setFontHeightInPoints((short) 12);
            CellStyle organizationStyle = workbook.createCellStyle();
            organizationStyle.setFont(boldFont);
            organizationStyle.setAlignment(HorizontalAlignment.CENTER);

            createMergedTitle(sheet, 0, 0, 2, "BỘ CÔNG THƯƠNG", organizationStyle);
            createMergedTitle(sheet, 1, 0, 2, "TRƯỜNG ĐẠI HỌC CÔNG NGHIỆP VIỆT - HUNG", organizationStyle);
            createMergedCell(sheet, 0, 3, 5, "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", organizationStyle);
            createMergedCell(sheet, 1, 3, 5, "Độc lập - Tự do - Hạnh phúc", organizationStyle);

            Font headingFont = workbook.createFont();
            headingFont.setBold(true);
            headingFont.setFontHeightInPoints((short) 15);
            CellStyle headingStyle = workbook.createCellStyle();
            headingStyle.setFont(headingFont);
            headingStyle.setAlignment(HorizontalAlignment.CENTER);
            createMergedTitle(sheet, 3, 0, 5, "DANH SÁCH SINH VIÊN THAM GIA ĐỒ ÁN TỐT NGHIỆP", headingStyle);
            createMergedTitle(sheet, 4, 0, 5, "Năm học: " + year, organizationStyle);

            CellStyle headerStyle = borderedStyle(workbook);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFont(boldFont);
            Row header = sheet.createRow(6);
            String[] labels = {"STT", "Mã sinh viên", "Họ và tên", "Lớp", "Email", "Số điện thoại"};
            for (int index = 0; index < labels.length; index++) {
                Cell cell = header.createCell(index);
                cell.setCellValue(labels[index]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle bodyStyle = borderedStyle(workbook);
            for (int index = 0; index < students.size(); index++) {
                StudentEntity student = students.get(index);
                Row row = sheet.createRow(7 + index);
                String className = student.getClassEntity() == null
                        ? ""
                        : student.getClassEntity().getClassName();
                String[] values = {
                    String.valueOf(index + 1),
                    student.getStudentCode(),
                    student.getFullNameStudent(),
                    className,
                    Objects.toString(student.getEmail(), ""),
                    Objects.toString(student.getPhoneStudent(), "")
                };
                for (int column = 0; column < values.length; column++) {
                    Cell cell = row.createCell(column);
                    cell.setCellValue(values[column]);
                    cell.setCellStyle(bodyStyle);
                }
            }
            sheet.createFreezePane(0, 7);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new AppException(ErrorCode.UKNOWN_ERROR);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public List<Integer> getStudentCreationYears() {
        return studentRepository.findDistinctCreationYears();
    }

    private void createMergedTitle(
            Sheet sheet, int rowIndex, int firstColumn, int lastColumn, String value, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        createMergedCell(sheet, row, rowIndex, firstColumn, lastColumn, value, style);
    }

    private void createMergedCell(
            Sheet sheet, int rowIndex, int firstColumn, int lastColumn, String value, CellStyle style) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) row = sheet.createRow(rowIndex);
        createMergedCell(sheet, row, rowIndex, firstColumn, lastColumn, value, style);
    }

    private void createMergedCell(
            Sheet sheet, Row row, int rowIndex, int firstColumn, int lastColumn, String value, CellStyle style) {
        Cell cell = row.createCell(firstColumn);
        cell.setCellValue(value);
        cell.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, firstColumn, lastColumn));
    }

    private CellStyle borderedStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    public record ImportStudentResult(
            int totalRows,
            int successRows,
            int failedRows,
            List<RegisterStudentResponse> importedStudents,
            List<ImportStudentError> errors) {}

    public record ImportStudentError(int row, String userName, String message) {}

    private record PendingStudentImport(int row, RegisterStudentRequest request) {}
}

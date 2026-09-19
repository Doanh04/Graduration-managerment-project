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
    // Hàm registerLecturer: Nhận RegisterLectureRequest; chuẩn hóa/kiểm tra dữ liệu, tải role giảng viên, tạo
    // UserEntity và LectureEntity rồi lưu hồ sơ giảng viên với sẵn quyền hướng dẫn và phản biện.
    public RegisterLectureResponse registerLecturer(RegisterLectureRequest request) {
        normalizeRequest(request);
        validateRequest(request);
        validateUniqueness(request);

        Roles supervisorRole = roleRepository
                .findById(RoleConstain.SUPERVISOR)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        Roles reviewerRole = roleRepository
                .findById(RoleConstain.REVIEWER)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        UserEntity user = userMaper.toUserEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(StatusConstain.ACTIVE);
        user.setCreateAt(LocalDateTime.now());
        user.setRoles(new HashSet<>(Set.of(supervisorRole, reviewerRole)));
        user = userRepository.save(user);

        LectureEntity lecturer = userMaper.toLecturerEntity(request);
        lecturer.setUser(user);
        lecturer = lectureRepository.save(lecturer);

        user.setLecture(lecturer);
        return userMaper.toLectureResponse(user, lecturer);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @Transactional
    // Hàm deleteLecturerAccount: Nhận mã bản ghi của deleteLecturerAccount, kiểm tra quyền và các quan hệ đang sử dụng,
    // sau đó xóa hoặc chuyển bản ghi sang trạng thái tương ứng.
    public void deleteLecturerAccount(String userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setStatus(StatusConstain.DELETED);
        userRepository.save(user);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @Transactional
    // Hàm updateLecturer: Nhận mã bản ghi cùng dữ liệu cập nhật của updateLecturer, tải bản ghi hiện có, kiểm tra trạng
    // thái và ràng buộc rồi ghi các giá trị mới xuống repository.
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
    // Hàm getAllLecturers: Nhận các tham số lọc/phân trang của getAllLecturers, truy vấn dữ liệu phù hợp từ repository,
    // ánh xạ từng entity sang DTO và trả về cho giao diện.
    public List<RegisterLectureResponse> getAllLecturers() {
        return getAllLecturers(0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    // Hàm getAllLecturers: Nhận các tham số lọc/phân trang của getAllLecturers, truy vấn dữ liệu phù hợp từ repository,
    // ánh xạ từng entity sang DTO và trả về cho giao diện.
    public List<RegisterLectureResponse> getAllLecturers(Integer page, Integer size) {
        return lectureRepository.findAll(PaginationSupport.pageRequest(page, size)).stream()
                .map(lecturer -> userMaper.toLectureResponse(lecturer.getUser(), lecturer))
                .toList();
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    // Hàm getAllLecturersPage: Nhận các tham số lọc/phân trang của getAllLecturersPage, truy vấn dữ liệu phù hợp từ
    // repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public com.graduration.DTO.Response.PageResponse<RegisterLectureResponse> getAllLecturersPage(
            Integer page, Integer size) {
        return getAllLecturersPage(page, size, null);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    // Hàm getAllLecturersPage: Nhận các tham số lọc/phân trang của getAllLecturersPage, truy vấn dữ liệu phù hợp từ
    // repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public com.graduration.DTO.Response.PageResponse<RegisterLectureResponse> getAllLecturersPage(
            Integer page, Integer size, String keyword) {
        var pageable = PaginationSupport.pageRequest(page, size);
        var lecturers = keyword == null || keyword.isBlank()
                ? lectureRepository.findAll(pageable)
                : lectureRepository.searchByNameOrCode(keyword.trim(), pageable);
        return com.graduration.DTO.Response.PageResponse.from(
                lecturers, lecturer -> userMaper.toLectureResponse(lecturer.getUser(), lecturer));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    // Hàm getLecturerByUserName: Nhận mã hoặc điều kiện tìm kiếm của getLecturerByUserName, truy vấn bản ghi/quan hệ
    // tương ứng, báo lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
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
    // Hàm resetPasswordByUserName: Nhận username giảng viên; tìm hồ sơ tương ứng, sinh mật khẩu tạm, mã hóa và cập nhật
    // UserEntity rồi trả thông tin đặt lại.
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
    // Hàm importLecturers: Nhận tệp hoặc dòng dữ liệu đầu vào của importLecturers, đọc các ô, kiểm tra định dạng và lỗi
    // nghiệp vụ rồi tạo danh sách dữ liệu hợp lệ để lưu.
    public ImportLectureResponse importLecturers(MultipartFile file) {
        validateExcelFile(file);

        List<RegisterLectureResponse> importedLecturers = new ArrayList<>();
        List<ImportLectureErrorResponse> errors = new ArrayList<>();
        List<PendingLecturerImport> pendingImports = new ArrayList<>();
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
                    pendingImports.add(new PendingLecturerImport(rowIndex + 1, readRequest(row, formatter)));
                } catch (RuntimeException exception) {
                    errors.add(ImportLectureErrorResponse.builder()
                            .row(rowIndex + 1)
                            .userName(cellValue(row, 0, formatter))
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

        ensureLecturersDoNotExist(pendingImports);
        for (PendingLecturerImport pending : pendingImports) {
            try {
                RegisterLectureResponse response =
                        transactionTemplate.execute(status -> registerLecturer(pending.request()));
                importedLecturers.add(response);
            } catch (RuntimeException exception) {
                errors.add(ImportLectureErrorResponse.builder()
                        .row(pending.row())
                        .userName(pending.request().getUserName())
                        .message(exception.getMessage())
                        .build());
            }
        }

        return ImportLectureResponse.builder()
                .totalRows(totalRows)
                .successRows(importedLecturers.size())
                .failedRows(errors.size())
                .importedLecturers(importedLecturers)
                .errors(errors)
                .build();
    }

    // Hàm ensureLecturersDoNotExist: Nhận đối tượng và điều kiện nghiệp vụ của ensureLecturersDoNotExist, đối chiếu các
    // quan hệ/trạng thái cần thiết và trả kết quả hoặc ném lỗi khi điều kiện không đạt.
    private void ensureLecturersDoNotExist(List<PendingLecturerImport> pendingImports) {
        Set<String> userNames = new HashSet<>();
        Set<String> lecturerCodes = new HashSet<>();
        Set<String> emails = new HashSet<>();
        Set<String> phones = new HashSet<>();
        for (PendingLecturerImport pending : pendingImports) {
            RegisterLectureRequest request = pending.request();
            normalizeRequest(request);
            if (isDuplicate(userNames, request.getUserName())
                    || isDuplicate(lecturerCodes, request.getLectureCode())
                    || isDuplicate(emails, request.getEmail())
                    || isDuplicate(phones, request.getPhone())
                    || userRepository.existsByUserName(request.getUserName())
                    || lectureRepository.existsByLectureCode(request.getLectureCode())
                    || (request.getEmail() != null && lectureRepository.existsByEmaillecture(request.getEmail()))
                    || (request.getPhone() != null && lectureRepository.existsByPhoneLecture(request.getPhone()))) {
                throw new AppException(ErrorCode.IMPORT_DATA_ALREADY_EXISTS);
            }
        }
    }

    // Hàm isDuplicate: Nhận đối tượng và điều kiện nghiệp vụ của isDuplicate, đối chiếu các quan hệ/trạng thái cần
    // thiết và trả kết quả hoặc ném lỗi khi điều kiện không đạt.
    private boolean isDuplicate(Set<String> values, String value) {
        return value != null && !values.add(value.toLowerCase(Locale.ROOT));
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
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

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
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

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
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

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
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

    // Hàm normalizeRequest: Nhận RegisterLectureRequest; trim username, mã giảng viên và họ tên, chuẩn hóa bằng
    // cấp/email/số điện thoại tùy chọn trước khi tạo hồ sơ.
    private void normalizeRequest(RegisterLectureRequest request) {
        request.setUserName(trim(request.getUserName()));
        request.setLectureCode(trim(request.getLectureCode()));
        request.setFullName(trim(request.getFullName()));
        request.setDegree(normalize(request.getDegree()));
        request.setEmail(normalize(request.getEmail()));
        request.setPhone(normalize(request.getPhone()));
    }

    // Hàm normalizeUpdateRequest: Nhận UpdateLecturerRequest; trim username, mã giảng viên và họ tên, chuẩn hóa các
    // trường bằng cấp/email/số điện thoại trước khi cập nhật.
    private void normalizeUpdateRequest(UpdateLecturerRequest request) {
        request.setUserName(trim(request.getUserName()));
        request.setLectureCode(trim(request.getLectureCode()));
        request.setFullName(trim(request.getFullName()));
        request.setDegree(normalize(request.getDegree()));
        request.setEmail(normalize(request.getEmail()));
        request.setPhone(normalize(request.getPhone()));
    }

    // Hàm trim: Nhận chuỗi mã hoặc tên từ request/ô Excel; giữ nguyên null và loại bỏ khoảng trắng đầu/cuối để dùng cho
    // kiểm tra trùng và ghi cơ sở dữ liệu.
    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    // Hàm normalize: Nhận bằng cấp, email hoặc số điện thoại tùy chọn của giảng viên; chuyển giá trị null/rỗng thành
    // null và trim phần có nội dung trước khi kiểm tra/lưu.
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void validateExcelFile(MultipartFile file) {
        if (file == null
                || file.isEmpty()
                || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new AppException(ErrorCode.INVALID_EXCEL_FILE);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
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

    // Hàm readRequest: Nhận một dòng Excel giảng viên; đọc username, mã giảng viên, họ tên, bằng cấp, email và điện
    // thoại để tạo RegisterLectureRequest.
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

    // Hàm cellValue: Nhận ô Excel danh sách giảng viên; đọc đúng kiểu dữ liệu và chuyển thành chuỗi dùng để tạo
    // RegisterLectureRequest hoặc báo lỗi dòng.
    private String cellValue(Row row, int column, DataFormatter formatter) {
        return normalize(formatter.formatCellValue(row.getCell(column)));
    }

    // Hàm isEmptyRow: Nhận một dòng Excel; kiểm tra toàn bộ ô có rỗng hoặc chỉ chứa khoảng trắng hay không để bỏ qua
    // dòng không có dữ liệu.
    private boolean isEmptyRow(Row row, DataFormatter formatter) {
        for (int column = 0; column <= 6; column++) {
            if (!formatter.formatCellValue(row.getCell(column)).isBlank()) {
                return false;
            }
        }
        return true;
    }

    // Hàm PendingLecturerImport: Đóng gói dữ liệu giảng viên đọc từ một dòng Excel trước khi kiểm tra trùng username/mã
    // và lưu tài khoản.
    private record PendingLecturerImport(int row, RegisterLectureRequest request) {}
}

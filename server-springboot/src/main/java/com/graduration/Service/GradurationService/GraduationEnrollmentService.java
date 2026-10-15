package com.graduration.Service.GradurationService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.EnrollmentStatusConstain;
import com.graduration.Constain.NotificationTypeConstain;
import com.graduration.Constain.ScoreStatusConstain;
import com.graduration.Constain.SupervisorAssignmentStatusConstain;
import com.graduration.Constain.SupervisorRoleConstain;
import com.graduration.DTO.Request.BulkCreateGraduationEnrollmentRequest;
import com.graduration.DTO.Request.CreateGraduationEnrollmentRequest;
import com.graduration.DTO.Request.UpdateGraduationEnrollmentRequest;
import com.graduration.DTO.Response.GraduationEnrollmentResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.Repository.DefensePeriodRepository;
import com.graduration.Repository.GraduationEnrollmentRepository;
import com.graduration.Repository.ScoreRepository;
import com.graduration.Repository.StudentRepository;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.GraduationEnrollmentEntity;
import com.graduration.entity.StudentEntity;
import com.graduration.entity.TeamEntity;
import com.graduration.entity.TopicSuperVisorEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.Service.NotificationService.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GraduationEnrollmentService {
    private final GraduationEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final DefensePeriodRepository defensePeriodRepository;
    private final ScoreRepository scoreRepository;
    private final NotificationService notificationService;

    /**
     * Đồng bộ hồ sơ ghi danh khi sinh viên được thêm vào nhóm hoặc khi đề xuất
     * của nhóm được duyệt. Hồ sơ đã bị hủy tư cách không được tự động kích hoạt
     * lại bởi các thao tác cập nhật nhóm sau đó.
     */
    @Transactional
    public void autoEnrollStudents(Collection<StudentEntity> students, DefensePeriodEntity period, String note) {
        if (students == null || students.isEmpty() || period == null) {
            return;
        }
        requireOpenPeriod(period);
        for (StudentEntity student : students) {
            GraduationEnrollmentEntity enrollment = enrollmentRepository
                    .findByStudent_IdStudentAndDefensePeriod_ID_Defense(student.getIdStudent(), period.getID_Defense())
                    .orElse(null);
            if (enrollment == null) {
                enrollmentRepository.save(GraduationEnrollmentEntity.builder()
                        .student(student)
                        .defensePeriod(period)
                        .status(EnrollmentStatusConstain.ENROLLED)
                        .note(normalize(note))
                        .build());
            } else if (enrollment.getStatus() == EnrollmentStatusConstain.ELIGIBLE) {
                enrollment.setStatus(EnrollmentStatusConstain.ENROLLED);
                enrollment.setNote(normalize(note));
                enrollment.setCompletedAt(null);
                enrollmentRepository.save(enrollment);
            }
        }
    }

    /** Tạo hồ sơ đủ điều kiện để làm khóa ngoại cho đề xuất, nhưng chưa ghi danh trước khi Admin duyệt. */
    @Transactional
    public GraduationEnrollmentEntity ensureEligible(StudentEntity student, DefensePeriodEntity period, String note) {
        requireOpenPeriod(period);
        return enrollmentRepository
                .findByStudent_IdStudentAndDefensePeriod_ID_Defense(student.getIdStudent(), period.getID_Defense())
                .orElseGet(() -> enrollmentRepository.save(GraduationEnrollmentEntity.builder()
                        .student(student)
                        .defensePeriod(period)
                        .status(EnrollmentStatusConstain.ELIGIBLE)
                        .note(normalize(note))
                        .build()));
    }

    /** Chặn mọi thao tác nghiệp vụ của sinh viên đã bị hủy tư cách trong đúng đợt. */
    @Transactional(readOnly = true)
    public void requireParticipationAllowed(String studentId, Long defensePeriodId) {
        enrollmentRepository
                .findByStudent_IdStudentAndDefensePeriod_ID_Defense(studentId, defensePeriodId)
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatusConstain.WITHDRAWN)
                .ifPresent(enrollment -> {
                    throw new AppException(ErrorCode.ENROLLMENT_WITHDRAWN);
                });
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm create: Nhận dữ liệu đầu vào của create, kiểm tra các trường bắt buộc và quan hệ liên quan, tạo bản ghi
    // nghiệp vụ rồi lưu repository để trả kết quả cho API.
    public GraduationEnrollmentResponse create(CreateGraduationEnrollmentRequest request) {
        StudentEntity student = studentRepository
                .findByStudentCodeIgnoreCase(request.getStudentCode().trim())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        DefensePeriodEntity period = defensePeriodRepository
                .findById(request.getDefensePeriodId())
                .orElseThrow(() -> new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND));
        ensureStudentHasTeam(student, period.getID_Defense());
        if (period.isFinished()) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_FINISHED);
        }
        if (enrollmentRepository.existsByStudent_IdStudentAndDefensePeriod_ID_Defense(
                student.getIdStudent(), period.getID_Defense())) {
            throw new AppException(ErrorCode.ENROLLMENT_ALREADY_EXISTS);
        }
        GraduationEnrollmentEntity enrollment = GraduationEnrollmentEntity.builder()
                .student(student)
                .defensePeriod(period)
                .status(EnrollmentStatusConstain.ENROLLED)
                .note(normalize(request.getNote()))
                .build();
        return toResponse(enrollmentRepository.save(enrollment));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm createBulk: Nhận dữ liệu đầu vào của createBulk, kiểm tra các trường bắt buộc và quan hệ liên quan, tạo bản
    // ghi nghiệp vụ rồi lưu repository để trả kết quả cho API.
    public List<GraduationEnrollmentResponse> createBulk(BulkCreateGraduationEnrollmentRequest request) {
        DefensePeriodEntity period = defensePeriodRepository
                .findById(request.getDefensePeriodId())
                .orElseThrow(() -> new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND));
        if (period.isFinished()) throw new AppException(ErrorCode.DEFENSE_PERIOD_FINISHED);
        List<String> codes = request.getStudentCodes().stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (codes.isEmpty()) throw new AppException(ErrorCode.USER_NOT_FOUND);
        List<StudentEntity> students = codes.stream()
                .map(code -> studentRepository
                        .findByStudentCodeIgnoreCase(code)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)))
                .toList();
        students.forEach(student -> ensureStudentHasTeam(student, period.getID_Defense()));
        if (students.stream()
                .anyMatch(student -> enrollmentRepository.existsByStudent_IdStudentAndDefensePeriod_ID_Defense(
                        student.getIdStudent(), period.getID_Defense()))) {
            throw new AppException(ErrorCode.ENROLLMENT_ALREADY_EXISTS);
        }
        List<GraduationEnrollmentEntity> enrollments = students.stream()
                .map(student -> GraduationEnrollmentEntity.builder()
                        .student(student)
                        .defensePeriod(period)
                        .status(EnrollmentStatusConstain.ENROLLED)
                        .note(normalize(request.getNote()))
                        .build())
                .toList();
        return enrollmentRepository.saveAll(enrollments).stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    // Hàm getAll: Nhận các tham số lọc/phân trang của getAll, truy vấn dữ liệu phù hợp từ repository, ánh xạ từng
    // entity sang DTO và trả về cho giao diện.
    public PageResponse<GraduationEnrollmentResponse> getAll(
            Long defensePeriodId, EnrollmentStatusConstain status, String keyword, Integer page, Integer size) {
        Specification<GraduationEnrollmentEntity> specification = Specification.where(null);
        if (defensePeriodId != null) {
            specification = specification.and(
                    (root, query, cb) -> cb.equal(root.get("defensePeriod").get("ID_Defense"), defensePeriodId));
        }
        if (status != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("student").get("studentCode")), pattern),
                    cb.like(cb.lower(root.get("student").get("fullNameStudent")), pattern),
                    cb.like(cb.lower(root.get("defensePeriod").get("periodName")), pattern)));
        }
        return PageResponse.from(
                enrollmentRepository.findAll(
                        specification,
                        PaginationSupport.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "enrolledAt"))),
                this::toResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    public byte[] exportByDefensePeriod(Long defensePeriodId) {
        if (defensePeriodId == null || defensePeriodId <= 0) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        DefensePeriodEntity period = defensePeriodRepository
                .findById(defensePeriodId)
                .orElseThrow(() -> new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND));
        List<GraduationEnrollmentEntity> enrollments =
                enrollmentRepository.findForExportByDefensePeriod(defensePeriodId, EnrollmentStatusConstain.WITHDRAWN);
        return createEnrollmentWorkbook(period, enrollments);
    }

    private byte[] createEnrollmentWorkbook(DefensePeriodEntity period, List<GraduationEnrollmentEntity> enrollments) {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Danh sách ghi danh");
            int[] widths = {8, 18, 30, 18, 42, 34};
            for (int index = 0; index < widths.length; index++) {
                sheet.setColumnWidth(index, widths[index] * 256);
            }
            sheet.setMargin(Sheet.LeftMargin, 0.35);
            sheet.setMargin(Sheet.RightMargin, 0.35);
            sheet.getPrintSetup().setLandscape(true);
            sheet.setFitToPage(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.getPrintSetup().setFitHeight((short) 0);

            Font organizationFont = excelFont(workbook, (short) 12, true);
            CellStyle organizationStyle = workbook.createCellStyle();
            organizationStyle.setFont(organizationFont);
            organizationStyle.setAlignment(HorizontalAlignment.CENTER);
            organizationStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            createMergedCell(sheet, 0, 0, 2, "BỘ CÔNG THƯƠNG", organizationStyle);
            createMergedCell(sheet, 1, 0, 2, "TRƯỜNG ĐẠI HỌC CÔNG NGHIỆP VIỆT-HUNG", organizationStyle);
            createMergedCell(sheet, 0, 3, 5, "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", organizationStyle);
            createMergedCell(sheet, 1, 3, 5, "Độc lập - Tự do - Hạnh phúc", organizationStyle);

            Font titleFont = excelFont(workbook, (short) 15, true);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            titleStyle.setWrapText(true);
            String academicYear = period.getAcademicYear() == null
                    ? ""
                    : Objects.toString(period.getAcademicYear().getAcademicYear(), "");
            String title = "DANH SÁCH SINH VIÊN GHI DANH ĐỒ ÁN TỐT NGHIỆP "
                    + Objects.toString(period.getPeriodName(), "")
                    + (academicYear.isBlank() ? "" : " NĂM " + academicYear);
            createMergedCell(sheet, 3, 0, 5, title, titleStyle);
            sheet.getRow(3).setHeightInPoints(34);

            CellStyle headerStyle = borderedStyle(workbook);
            headerStyle.setFont(excelFont(workbook, (short) 11, true));
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setWrapText(true);
            Row header = sheet.createRow(5);
            header.setHeightInPoints(30);
            String[] columns = {
                "STT", "Mã sinh viên", "Tên sinh viên", "Số điện thoại", "Tên đề tài", "Giảng viên hướng dẫn"
            };
            for (int index = 0; index < columns.length; index++) {
                Cell cell = header.createCell(index);
                cell.setCellValue(columns[index]);
                cell.setCellStyle(headerStyle);
            }

            CellStyle centeredBodyStyle = borderedStyle(workbook);
            centeredBodyStyle.setFont(excelFont(workbook, (short) 11, false));
            centeredBodyStyle.setAlignment(HorizontalAlignment.CENTER);
            centeredBodyStyle.setWrapText(true);
            CellStyle bodyStyle = borderedStyle(workbook);
            bodyStyle.setFont(excelFont(workbook, (short) 11, false));
            bodyStyle.setWrapText(true);

            for (int index = 0; index < enrollments.size(); index++) {
                GraduationEnrollmentEntity enrollment = enrollments.get(index);
                StudentEntity student = enrollment.getStudent();
                TeamEntity team = teamForPeriod(student, period.getID_Defense());
                String topicTitle = team == null || team.getTopic() == null
                        ? ""
                        : Objects.toString(team.getTopic().getTitle(), "");
                String supervisors = team == null || team.getTopic() == null
                        ? ""
                        : team.getTopic().getTopicSuperVisorEntities().stream()
                                .filter(assignment ->
                                        assignment.getStatus() == SupervisorAssignmentStatusConstain.ACTIVE)
                                .sorted(Comparator.comparingInt((TopicSuperVisorEntity assignment) ->
                                                assignment.getSupervisorRole() == SupervisorRoleConstain.PRIMARY
                                                        ? 0
                                                        : 1)
                                        .thenComparing(assignment -> Objects.toString(
                                                assignment.getLecture().getFullNameLecture(), "")))
                                .map(assignment -> Objects.toString(
                                        assignment.getLecture().getFullNameLecture(),
                                        assignment.getLecture().getLectureCode()))
                                .filter(name -> !name.isBlank())
                                .distinct()
                                .reduce((left, right) -> left + ", " + right)
                                .orElse("");
                String[] values = {
                    String.valueOf(index + 1),
                    Objects.toString(student.getStudentCode(), ""),
                    Objects.toString(student.getFullNameStudent(), ""),
                    Objects.toString(student.getPhoneStudent(), ""),
                    topicTitle,
                    supervisors
                };
                Row row = sheet.createRow(6 + index);
                row.setHeightInPoints(32);
                for (int column = 0; column < values.length; column++) {
                    Cell cell = row.createCell(column);
                    cell.setCellValue(values[column]);
                    cell.setCellStyle(column <= 1 || column == 3 ? centeredBodyStyle : bodyStyle);
                }
            }
            sheet.createFreezePane(0, 6);
            sheet.setRepeatingRows(new CellRangeAddress(5, 5, -1, -1));
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new AppException(ErrorCode.UKNOWN_ERROR);
        }
    }

    private Font excelFont(Workbook workbook, short size, boolean bold) {
        Font font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setFontHeightInPoints(size);
        font.setBold(bold);
        return font;
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

    private void createMergedCell(
            Sheet sheet, int rowIndex, int firstColumn, int lastColumn, String value, CellStyle style) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) row = sheet.createRow(rowIndex);
        Cell cell = row.createCell(firstColumn);
        cell.setCellValue(value);
        cell.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, firstColumn, lastColumn));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    // Hàm get: Nhận mã hoặc điều kiện tìm kiếm của get, truy vấn bản ghi/quan hệ tương ứng, báo lỗi khi không tồn tại
    // và trả về dữ liệu đã ánh xạ.
    public GraduationEnrollmentResponse get(Long id) {
        return toResponse(find(id));
    }

    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Transactional(readOnly = true)
    // Hàm getMine: Nhận các tham số lọc/phân trang của getMine, truy vấn dữ liệu phù hợp từ repository, ánh xạ từng
    // entity sang DTO và trả về cho giao diện.
    public List<GraduationEnrollmentResponse> getMine() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return enrollmentRepository.findByStudent_UserEntity_UserIdOrderByEnrolledAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm update: Nhận mã bản ghi cùng dữ liệu cập nhật của update, tải bản ghi hiện có, kiểm tra trạng thái và ràng
    // buộc rồi ghi các giá trị mới xuống repository.
    public GraduationEnrollmentResponse update(Long id, UpdateGraduationEnrollmentRequest request) {
        GraduationEnrollmentEntity enrollment = find(id);
        requireOpenPeriod(enrollment.getDefensePeriod());
        EnrollmentStatusConstain next = request.getStatus();
        if (next != enrollment.getStatus()
                && !allowedNext(enrollment.getStatus()).contains(next)) {
            throw new AppException(ErrorCode.ENROLLMENT_OPERATION_NOT_ALLOWED);
        }
        enrollment.setStatus(next);
        enrollment.setNote(normalize(request.getNote()));
        enrollment.setCompletedAt(isTerminal(next) ? LocalDateTime.now() : null);
        return toResponse(enrollmentRepository.save(enrollment));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public GraduationEnrollmentResponse withdraw(Long id) {
        GraduationEnrollmentEntity enrollment = find(id);
        requireOpenPeriod(enrollment.getDefensePeriod());
        if (enrollment.getStatus() == EnrollmentStatusConstain.WITHDRAWN) {
            return toResponse(enrollment);
        }
        enrollment.setStatus(EnrollmentStatusConstain.WITHDRAWN);
        enrollment.setCompletedAt(LocalDateTime.now());
        enrollment.setNote("Đã bị hủy tư cách thi");
        LocalDateTime publishedAt = LocalDateTime.now();
        scoreRepository
                .findByStudentAndDefensePeriod(
                        enrollment.getStudent().getIdStudent(),
                        enrollment.getDefensePeriod().getID_Defense())
                .forEach(score -> {
                    score.setScore(BigDecimal.ZERO.setScale(2));
                    score.setComment("Sinh viên bị hủy tư cách thi");
                    score.setStatus(ScoreStatusConstain.LOCKED);
                    score.setPublishedAt(publishedAt);
                });
        GraduationEnrollmentEntity saved = enrollmentRepository.save(enrollment);
        if (saved.getStudent().getUserEntity() != null) {
            notificationService.notifyUsersOnce(
                    List.of(saved.getStudent().getUserEntity().getUserId()),
                    "Thông báo hủy tư cách thi",
                    "Bạn đã bị hủy tư cách thi trong đợt \"" + saved.getDefensePeriod().getPeriodName()
                            + "\". Điểm của đợt này được ghi nhận là 0.",
                    NotificationTypeConstain.DEFENSE_BANNED,
                    "GRADUATION_ENROLLMENT",
                    saved.getEnrollmentId());
        }
        return toResponse(saved);
    }

    // Hàm find: Nhận mã hoặc điều kiện tìm kiếm của find, truy vấn bản ghi/quan hệ tương ứng, báo lỗi khi không tồn tại
    // và trả về dữ liệu đã ánh xạ.
    private GraduationEnrollmentEntity find(Long id) {
        return enrollmentRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));
    }

    private void requireOpenPeriod(DefensePeriodEntity period) {
        if (period == null) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND);
        }
        if (period.isFinished()) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_FINISHED);
        }
    }

    // Hàm ensureStudentHasTeam: kiểm tra quan hệ nhóm hiện tại của sinh viên trước khi cho phép tạo hồ sơ ghi danh;
    // sinh viên chưa được gán vào nhóm sẽ bị từ chối để hồ sơ luôn xác định được nhóm thực hiện đồ án.
    private void ensureStudentHasTeam(StudentEntity student, Long defensePeriodId) {
        if (teamForPeriod(student, defensePeriodId) == null) {
            throw new AppException(ErrorCode.ENROLLMENT_STUDENT_TEAM_REQUIRED);
        }
    }

    // Hàm allowedNext: Nhận trạng thái ghi danh hiện tại; trả về tập trạng thái chuyển tiếp hợp lệ để API chỉ cho phép
    // luồng nghiệp vụ đúng thứ tự.
    private EnumSet<EnrollmentStatusConstain> allowedNext(EnrollmentStatusConstain current) {
        return switch (current) {
            case ELIGIBLE -> EnumSet.of(EnrollmentStatusConstain.ENROLLED);
            case ENROLLED -> EnumSet.of(EnrollmentStatusConstain.IN_PROGRESS);
            case IN_PROGRESS -> EnumSet.of(EnrollmentStatusConstain.COMPLETED, EnrollmentStatusConstain.FAILED);
            case COMPLETED -> EnumSet.of(EnrollmentStatusConstain.FAILED);
            case FAILED -> EnumSet.of(EnrollmentStatusConstain.COMPLETED);
                // Giữ khả năng đọc dữ liệu cũ nhưng không cho phép chọn trạng thái này nữa.
            case WITHDRAWN -> EnumSet.noneOf(EnrollmentStatusConstain.class);
        };
    }

    // Hàm isTerminal: Nhận trạng thái ghi danh; xác định trạng thái đã hoàn tất hoặc không đạt là trạng thái kết thúc,
    // không còn chuyển tiếp tự động.
    private boolean isTerminal(EnrollmentStatusConstain status) {
        return status == EnrollmentStatusConstain.COMPLETED || status == EnrollmentStatusConstain.FAILED;
    }

    // Hàm normalize: Nhận ghi chú ghi danh; chuyển chuỗi null/rỗng thành null và trim nội dung trước khi lưu hồ sơ sinh
    // viên vào đợt bảo vệ.
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // Hàm toResponse: Nhận GraduationEnrollmentEntity; lấy sinh viên, đợt bảo vệ và trạng thái ghi danh rồi tạo DTO
    // hiển thị cho màn hình ghi danh.
    private GraduationEnrollmentResponse toResponse(GraduationEnrollmentEntity enrollment) {
        var student = enrollment.getStudent();
        var period = enrollment.getDefensePeriod();
        return GraduationEnrollmentResponse.builder()
                .enrollmentId(enrollment.getEnrollmentId())
                .studentId(student.getIdStudent())
                .studentCode(student.getStudentCode())
                .studentName(student.getFullNameStudent())
                .classCode(
                        student.getClassEntity() == null
                                ? null
                                : student.getClassEntity().getClassCode())
                .teamId(
                        teamForPeriod(student, period.getID_Defense()) == null
                                ? null
                                : teamForPeriod(student, period.getID_Defense()).getIdTeam())
                .teamName(
                        teamForPeriod(student, period.getID_Defense()) == null
                                ? null
                                : teamForPeriod(student, period.getID_Defense()).getNameTeam())
                .defensePeriodId(period.getID_Defense())
                .defensePeriodName(period.getPeriodName())
                .academicYear(
                        period.getAcademicYear() == null
                                ? null
                                : period.getAcademicYear().getAcademicYear())
                .status(enrollment.getStatus())
                .enrolledAt(enrollment.getEnrolledAt())
                .completedAt(enrollment.getCompletedAt())
                .note(enrollment.getNote())
                .build();
    }

    private com.graduration.entity.TeamEntity teamForPeriod(StudentEntity student, Long defensePeriodId) {
        return student.getTeamMemberships().stream()
                .filter(team -> team.getDefensePeriod() != null
                        && defensePeriodId.equals(team.getDefensePeriod().getID_Defense()))
                .findFirst()
                .orElse(null);
    }
}

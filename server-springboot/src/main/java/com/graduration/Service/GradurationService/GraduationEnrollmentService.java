package com.graduration.Service.GradurationService;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.Constain.EnrollmentStatusConstain;
import com.graduration.DTO.Request.BulkCreateGraduationEnrollmentRequest;
import com.graduration.DTO.Request.CreateGraduationEnrollmentRequest;
import com.graduration.DTO.Request.UpdateGraduationEnrollmentRequest;
import com.graduration.DTO.Response.GraduationEnrollmentResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.Repository.DefensePeriodRepository;
import com.graduration.Repository.GraduationEnrollmentRepository;
import com.graduration.Repository.StudentRepository;
import com.graduration.Repository.ScoreRepository;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.GraduationEnrollmentEntity;
import com.graduration.entity.StudentEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GraduationEnrollmentService {
    private final GraduationEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final DefensePeriodRepository defensePeriodRepository;
    private final ScoreRepository scoreRepository;

    /**
     * Đồng bộ hồ sơ ghi danh khi sinh viên được thêm vào nhóm hoặc khi đề xuất
     * của nhóm được duyệt. Hồ sơ đã bị hủy tư cách không được tự động kích hoạt
     * lại bởi các thao tác cập nhật nhóm sau đó.
     */
    @Transactional
    public void autoEnrollStudents(
            Collection<StudentEntity> students, DefensePeriodEntity period, String note) {
        if (students == null || students.isEmpty() || period == null) {
            return;
        }
        for (StudentEntity student : students) {
            GraduationEnrollmentEntity enrollment = enrollmentRepository
                    .findByStudent_IdStudentAndDefensePeriod_ID_Defense(
                            student.getIdStudent(), period.getID_Defense())
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
        if (period.getStatus() == DefensePeriodConstain.FINISHED) {
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
        if (period.getStatus() == DefensePeriodConstain.FINISHED)
            throw new AppException(ErrorCode.DEFENSE_PERIOD_FINISHED);
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
        if (enrollment.getStatus() == EnrollmentStatusConstain.WITHDRAWN) {
            return toResponse(enrollment);
        }
        enrollment.setStatus(EnrollmentStatusConstain.WITHDRAWN);
        enrollment.setCompletedAt(LocalDateTime.now());
        enrollment.setNote("Đã bị hủy tư cách thi");
        scoreRepository.findByStudentAndDefensePeriod(
                        enrollment.getStudent().getIdStudent(), enrollment.getDefensePeriod().getID_Defense())
                .forEach(score -> {
                    score.setScore(BigDecimal.ZERO.setScale(2));
                    score.setComment("Sinh viên bị hủy tư cách thi");
                });
        return toResponse(enrollmentRepository.save(enrollment));
    }

    // Hàm find: Nhận mã hoặc điều kiện tìm kiếm của find, truy vấn bản ghi/quan hệ tương ứng, báo lỗi khi không tồn tại
    // và trả về dữ liệu đã ánh xạ.
    private GraduationEnrollmentEntity find(Long id) {
        return enrollmentRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));
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

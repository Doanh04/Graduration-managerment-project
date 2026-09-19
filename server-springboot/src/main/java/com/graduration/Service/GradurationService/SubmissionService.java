package com.graduration.Service.GradurationService;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.CommentTypeConstain;
import com.graduration.Constain.MilesStoneStatusConstain;
import com.graduration.Constain.SubmissionStatusConstain;
import com.graduration.Constain.SupervisorAssignmentStatusConstain;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.SubmissionResponse;
import com.graduration.Repository.GraduationEnrollmentRepository;
import com.graduration.Repository.MilestoneRepository;
import com.graduration.Repository.StudentRepository;
import com.graduration.Repository.SubmissionRepository;
import com.graduration.Repository.TeamRepository;
import com.graduration.entity.MilesStoneEntity;
import com.graduration.entity.StudentEntity;
import com.graduration.entity.SubmistionEntity;
import com.graduration.entity.TeamEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.SubmissionMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubmissionService {
    private static final Set<String> BLOCKED_EXTENSIONS =
            Set.of("exe", "sh", "bat", "cmd", "jar", "php", "js", "html", "htm");

    SubmissionRepository submissionRepository;
    MilestoneRepository milestoneRepository;
    TeamRepository teamRepository;
    StudentRepository studentRepository;
    GraduationEnrollmentRepository enrollmentRepository;
    SubmissionMapper submissionMapper;
    CommentService commentService;
    GraduationEnrollmentService graduationEnrollmentService;

    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Transactional
    // Hàm upload: Nhận tệp nộp, mã mốc tiến độ và ghi chú; kiểm tra quyền, định dạng, thời hạn và phiên bản, lưu tệp
    // rồi tạo SubmissionEntity liên kết với nhóm/mốc.
    public SubmissionResponse upload(
            Long milestoneId, Long teamId, String note, MultipartFile file, LocalDateTime submittedAt) {
        validateFilePresent(file);
        MilesStoneEntity milestone = findMilestone(milestoneId);
        TeamEntity team = findTeam(teamId);
        StudentEntity student = currentStudent();
        requireTeamMember(team, student);
        requireSameDefensePeriod(team, student, milestone);

        LocalDateTime now = submittedAt == null ? LocalDateTime.now() : submittedAt;
        validateSubmissionWindow(milestone, now);
        String extension = extension(file.getOriginalFilename());
        validateFile(milestone, file, extension);
        SubmistionEntity latest = submissionRepository
                .findFirstByTeam_IdTeamAndMilesStone_IdMilesStoneOrderByVersionDesc(teamId, milestoneId)
                .orElse(null);
        if (latest != null && latest.getStatus() == SubmissionStatusConstain.APPROVED) {
            throw new AppException(ErrorCode.SUBMISSION_ALREADY_APPROVED);
        }

        byte[] fileData = readFileData(file);
        String originalName = safeOriginalName(file.getOriginalFilename());
        SubmistionEntity submission = SubmistionEntity.builder()
                .fileName(originalName)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .checksum(checksum(fileData))
                .fileData(fileData)
                .isLate(now.isAfter(milestone.getDeadLine()))
                .note(normalize(note))
                .submittedAt(now)
                .updatedAt(now)
                .version(latest == null ? 1 : latest.getVersion() + 1)
                .status(SubmissionStatusConstain.SUBMITTED)
                .submittedBy(student)
                .milesStone(milestone)
                .team(team)
                .build();
        return submissionMapper.toResponse(submissionRepository.save(submission));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    // Hàm getSubmission: Nhận mã hoặc điều kiện tìm kiếm của getSubmission, truy vấn bản ghi/quan hệ tương ứng, báo lỗi
    // khi không tồn tại và trả về dữ liệu đã ánh xạ.
    public SubmissionResponse getSubmission(Long submissionId) {
        SubmistionEntity submission = findSubmission(submissionId);
        requireReadAccess(submission);
        return submissionMapper.toResponse(submission);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    // Hàm getSubmissions: Nhận mã hoặc điều kiện tìm kiếm của getSubmissions, truy vấn bản ghi/quan hệ tương ứng, báo
    // lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    public PageResponse<SubmissionResponse> getSubmissions(
            Long teamId, Long milestoneId, SubmissionStatusConstain status, Boolean late, Integer page, Integer size) {
        Specification<SubmistionEntity> specification = Specification.where(null);
        if (teamId != null) {
            specification = specification.and(
                    (root, query, cb) -> cb.equal(root.get("team").get("idTeam"), teamId));
        }
        if (milestoneId != null) {
            specification = specification.and(
                    (root, query, cb) -> cb.equal(root.get("milesStone").get("IdMilesStone"), milestoneId));
        }
        if (status != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (late != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("isLate"), late));
        }
        specification = specification.and(accessSpecification());
        return PageResponse.from(
                submissionRepository.findAll(
                        specification,
                        PaginationSupport.pageRequest(
                                page, size, Sort.by(Sort.Order.desc("submittedAt"), Sort.Order.desc("version")))),
                submissionMapper::toResponse);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    // Hàm getVersionHistory: Nhận các tham số lọc/phân trang của getVersionHistory, truy vấn dữ liệu phù hợp từ
    // repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public PageResponse<SubmissionResponse> getVersionHistory(
            Long teamId, Long milestoneId, Integer page, Integer size) {
        TeamEntity team = findTeam(teamId);
        requireTeamReadAccess(team);
        findMilestone(milestoneId);
        return PageResponse.from(
                submissionRepository.findByTeam_IdTeamAndMilesStone_IdMilesStoneOrderByVersionDesc(
                        teamId, milestoneId, PaginationSupport.pageRequest(page, size)),
                submissionMapper::toResponse);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    // Hàm download: Nhận mã bài nộp; kiểm tra quyền xem, đọc tệp từ kho lưu trữ và trả về nội dung cùng tên tệp cho
    // client tải xuống.
    public DownloadedSubmission download(Long submissionId) {
        SubmistionEntity submission = findSubmission(submissionId);
        requireReadAccess(submission);
        return new DownloadedSubmission(
                new ByteArrayResource(submission.getFileData()), submission.getFileName(), submission.getContentType());
    }

    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Transactional
    // Hàm withdraw: Nhận mã bài nộp; kiểm tra người thao tác và trạng thái cho phép, đánh dấu rút bài/cập nhật thời
    // gian để bài không còn được dùng trong quy trình chấm.
    public SubmissionResponse withdraw(Long submissionId) {
        SubmistionEntity submission = findSubmission(submissionId);
        StudentEntity student = currentStudent();
        requireTeamMember(submission.getTeam(), student);
        graduationEnrollmentService.requireParticipationAllowed(
                student.getIdStudent(), submission.getMilesStone().getDefensePeriod().getID_Defense());
        SubmistionEntity latest = submissionRepository
                .findFirstByTeam_IdTeamAndMilesStone_IdMilesStoneOrderByVersionDesc(
                        submission.getTeam().getIdTeam(),
                        submission.getMilesStone().getIdMilesStone())
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));
        if (!latest.getIdSubmission().equals(submissionId)
                || submission.getStatus() != SubmissionStatusConstain.SUBMITTED) {
            throw new AppException(ErrorCode.SUBMISSION_OPERATION_NOT_ALLOWED);
        }
        submission.setStatus(SubmissionStatusConstain.WITHDRAWN);
        return submissionMapper.toResponse(submissionRepository.save(submission));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional
    // Hàm startReview: Nhận mã bản ghi và thông tin thao tác của startReview, kiểm tra trạng thái hiện tại cùng quyền
    // thực hiện, cập nhật trạng thái/lý do và lưu thay đổi.
    public SubmissionResponse startReview(Long submissionId) {
        SubmistionEntity submission = findSubmission(submissionId);
        requireReviewAccess(submission);
        requireStatus(submission, SubmissionStatusConstain.SUBMITTED);
        submission.setStatus(SubmissionStatusConstain.UNDER_REVIEW);
        return submissionMapper.toResponse(submissionRepository.save(submission));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional
    // Hàm requestRevision: Nhận assignment và yêu cầu chỉnh sửa; kiểm tra người sở hữu cùng trạng thái, lưu ghi chú/lý
    // do yêu cầu bổ sung và chuyển assignment về trạng thái cần xử lý lại.
    public SubmissionResponse requestRevision(Long submissionId, String comment) {
        return reviewWithComment(
                submissionId,
                comment,
                SubmissionStatusConstain.REVISION_REQUIRED,
                CommentTypeConstain.REVISION_REQUEST);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional
    // Hàm approve: Nhận mã bản ghi và thông tin thao tác của approve, kiểm tra trạng thái hiện tại cùng quyền thực
    // hiện, cập nhật trạng thái/lý do và lưu thay đổi.
    public SubmissionResponse approve(Long submissionId, String comment) {
        return reviewWithComment(
                submissionId, comment, SubmissionStatusConstain.APPROVED, CommentTypeConstain.APPROVAL);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional
    // Hàm reject: Nhận mã bản ghi và thông tin thao tác của reject, kiểm tra trạng thái hiện tại cùng quyền thực hiện,
    // cập nhật trạng thái/lý do và lưu thay đổi.
    public SubmissionResponse reject(Long submissionId, String comment) {
        return reviewWithComment(
                submissionId, comment, SubmissionStatusConstain.REJECTED, CommentTypeConstain.REJECTION);
    }

    // Hàm reviewWithComment: Nhận bài nộp, trạng thái đánh giá và nội dung nhận xét; kiểm tra quyền giảng viên, cập
    // nhật trạng thái review và lưu comment gắn với phiên bản.
    private SubmissionResponse reviewWithComment(
            Long submissionId, String comment, SubmissionStatusConstain target, CommentTypeConstain commentType) {
        validateComment(comment);
        SubmistionEntity submission = findSubmission(submissionId);
        requireReviewAccess(submission);
        if (submission.getStatus() != SubmissionStatusConstain.SUBMITTED
                && submission.getStatus() != SubmissionStatusConstain.UNDER_REVIEW) {
            throw new AppException(ErrorCode.SUBMISSION_OPERATION_NOT_ALLOWED);
        }
        submission.setStatus(target);
        commentService.createWorkflowComment(submission, comment, commentType);
        return submissionMapper.toResponse(submissionRepository.save(submission));
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void validateSubmissionWindow(MilesStoneEntity milestone, LocalDateTime now) {
        if (milestone.getStatus() != MilesStoneStatusConstain.OPEN) {
            throw new AppException(ErrorCode.SUBMISSION_NOT_OPEN);
        }
        if (now.isBefore(milestone.getStartAt())) {
            throw new AppException(ErrorCode.SUBMISSION_NOT_STARTED);
        }
        if (now.isAfter(milestone.getDeadLine()) && !Boolean.TRUE.equals(milestone.getAllowLateSubmission())) {
            throw new AppException(ErrorCode.SUBMISSION_DEADLINE_PASSED);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void validateFilePresent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.SUBMISSION_FILE_REQUIRED);
        }
    }

    private byte[] readFileData(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new AppException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    private String checksum(byte[] fileData) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(fileData));
        } catch (NoSuchAlgorithmException exception) {
            throw new AppException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void validateFile(MilesStoneEntity milestone, MultipartFile file, String extension) {
        if (milestone.getMaxFileSize() != null && file.getSize() > milestone.getMaxFileSize()) {
            throw new AppException(ErrorCode.SUBMISSION_FILE_TOO_LARGE);
        }
        if (extension.isBlank() || BLOCKED_EXTENSIONS.contains(extension)) {
            throw new AppException(ErrorCode.SUBMISSION_FILE_TYPE_NOT_ALLOWED);
        }
        String allowed = milestone.getAllowedFileTypes();
        if (allowed != null && !allowed.isBlank()) {
            boolean accepted = Arrays.stream(allowed.split(","))
                    .map(String::trim)
                    .map(value -> value.startsWith(".") ? value.substring(1) : value)
                    .map(value -> value.toLowerCase(Locale.ROOT))
                    .anyMatch(extension::equals);
            if (!accepted) {
                throw new AppException(ErrorCode.SUBMISSION_FILE_TYPE_NOT_ALLOWED);
            }
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireSameDefensePeriod(TeamEntity team, StudentEntity student, MilesStoneEntity milestone) {
        Long periodId = milestone.getDefensePeriod().getID_Defense();
        graduationEnrollmentService.requireParticipationAllowed(student.getIdStudent(), periodId);
        if (team.getTopic() != null) {
            if (!periodId.equals(team.getTopic().getDefensePeriod().getID_Defense())) {
                throw new AppException(ErrorCode.SUBMISSION_PERIOD_MISMATCH);
            }
            return;
        }
        if (!enrollmentRepository.existsByStudent_IdStudentAndDefensePeriod_ID_Defense(
                student.getIdStudent(), periodId)) {
            throw new AppException(ErrorCode.SUBMISSION_PERIOD_MISMATCH);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireReadAccess(SubmistionEntity submission) {
        if (isManager() || isTeamMember(submission.getTeam()) || isAssignedSupervisor(submission.getTeam())) {
            return;
        }
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireTeamReadAccess(TeamEntity team) {
        if (isManager() || isTeamMember(team) || isAssignedSupervisor(team)) {
            return;
        }
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireReviewAccess(SubmistionEntity submission) {
        if (isManager() || isAssignedSupervisor(submission.getTeam())) {
            return;
        }
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    // Hàm accessSpecification: Nhận bài nộp và Authentication; xây dựng quy tắc truy cập cho admin, giảng viên hướng
    // dẫn/phản biện và sinh viên thuộc nhóm.
    private Specification<SubmistionEntity> accessSpecification() {
        if (isManager()) {
            return Specification.where(null);
        }
        String userId = currentAuthentication().getName();
        if (hasAuthority("ROLE_STUDENT")) {
            return (root, query, cb) -> cb.equal(
                    root.join("team").join("studentEntities").join("userEntity").get("userId"), userId);
        }
        return (root, query, cb) -> {
            query.distinct(true);
            var supervisor = root.join("team").join("topic").join("topicSuperVisorEntities");
            return cb.and(
                    cb.equal(supervisor.get("status"), SupervisorAssignmentStatusConstain.ACTIVE),
                    cb.equal(supervisor.join("lecture").join("user").get("userId"), userId));
        };
    }

    // Hàm isAssignedSupervisor: Kiểm tra danh sách phân công của đề tài có giảng viên hiện tại ở trạng thái ACTIVE, từ
    // đó xác định quyền giảng viên hướng dẫn.
    private boolean isAssignedSupervisor(TeamEntity team) {
        String userId = currentAuthentication().getName();
        return team.getTopic() != null
                && team.getTopic().getTopicSuperVisorEntities().stream()
                        .anyMatch(supervisor -> supervisor.getLecture() != null
                                && supervisor.getStatus() == SupervisorAssignmentStatusConstain.ACTIVE
                                && supervisor.getLecture().getUser() != null
                                && userId.equals(
                                        supervisor.getLecture().getUser().getUserId()));
    }

    // Hàm isTeamMember: Đối chiếu userId hiện tại với danh sách sinh viên của nhóm đề tài để xác định người dùng có
    // thuộc nhóm hay không.
    private boolean isTeamMember(TeamEntity team) {
        String userId = currentAuthentication().getName();
        return team.getStudentEntities().stream()
                .anyMatch(student -> student.getUserEntity() != null
                        && userId.equals(student.getUserEntity().getUserId()));
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireTeamMember(TeamEntity team, StudentEntity student) {
        if (team.getStudentEntities().stream()
                .noneMatch(member -> member.getIdStudent().equals(student.getIdStudent()))) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    // Hàm currentStudent: Dùng userId hiện tại truy vấn UserEntity, kiểm tra tài khoản có hồ sơ sinh viên rồi trả về
    // StudentEntity cho các nghiệp vụ dành cho sinh viên.
    private StudentEntity currentStudent() {
        return studentRepository
                .findByUserEntity_UserId(currentAuthentication().getName())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));
    }

    // Hàm isManager: Kiểm tra Authentication hiện tại có quyền ROLE_ADMIN hoặc ROLE_FACULTY hay không để xác định người
    // dùng có quyền quản lý dữ liệu.
    private boolean isManager() {
        return hasAuthority("ROLE_ADMIN") || hasAuthority("ROLE_FACULTY");
    }

    // Hàm hasAuthority: Nhận tên quyền cần kiểm tra; so sánh với danh sách GrantedAuthority của tài khoản hiện tại và
    // trả về true nếu tài khoản có quyền đó.
    private boolean hasAuthority(String authority) {
        return currentAuthentication().getAuthorities().stream()
                .anyMatch(item -> item.getAuthority().equals(authority));
    }

    // Hàm currentAuthentication: Đọc Authentication từ SecurityContext của request hiện tại; từ chối khi chưa đăng nhập
    // và trả về đối tượng xác thực để lấy userId cùng quyền.
    private Authentication currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication;
    }

    // Hàm findMilestone: Nhận mã hoặc điều kiện tìm kiếm của findMilestone, truy vấn bản ghi/quan hệ tương ứng, báo lỗi
    // khi không tồn tại và trả về dữ liệu đã ánh xạ.
    private MilesStoneEntity findMilestone(Long milestoneId) {
        return milestoneRepository
                .findById(milestoneId)
                .orElseThrow(() -> new AppException(ErrorCode.MILESTONE_NOT_FOUND));
    }

    // Hàm findTeam: Nhận mã hoặc điều kiện tìm kiếm của findTeam, truy vấn bản ghi/quan hệ tương ứng, báo lỗi khi không
    // tồn tại và trả về dữ liệu đã ánh xạ.
    private TeamEntity findTeam(Long teamId) {
        return teamRepository
                .findWithDetailsByIdTeam(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_FOUND));
    }

    // Hàm findSubmission: Nhận mã hoặc điều kiện tìm kiếm của findSubmission, truy vấn bản ghi/quan hệ tương ứng, báo
    // lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    private SubmistionEntity findSubmission(Long submissionId) {
        return submissionRepository
                .findById(submissionId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireStatus(SubmistionEntity submission, SubmissionStatusConstain status) {
        if (submission.getStatus() != status) {
            throw new AppException(ErrorCode.SUBMISSION_OPERATION_NOT_ALLOWED);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void validateComment(String comment) {
        if (comment == null || comment.isBlank()) {
            throw new AppException(ErrorCode.SUBMISSION_COMMENT_NOT_BLANK);
        }
    }

    // Hàm safeOriginalName: Nhận tên tệp gốc do trình duyệt gửi lên; loại đường dẫn nguy hiểm và ký tự không an toàn,
    // sau đó trả về tên chỉ dùng làm metadata hiển thị.
    private String safeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "submission";
        }
        String normalized = originalName.replace('\\', '/');
        int separator = normalized.lastIndexOf('/');
        String fileName = separator < 0 ? normalized : normalized.substring(separator + 1);
        return fileName.isBlank() ? "submission" : fileName;
    }

    // Hàm extension: Nhận tên tệp; lấy phần mở rộng cuối cùng sau dấu chấm, chuyển về chữ thường để đối chiếu với danh
    // sách định dạng được phép.
    private String extension(String originalName) {
        String safeName = safeOriginalName(originalName);
        int index = safeName.lastIndexOf('.');
        return index < 0 ? "" : safeName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    // Hàm normalize: Nhận ghi chú của bài nộp từ form sinh viên; chuyển null hoặc chuỗi trắng thành null, trim nội dung
    // còn lại để lưu cùng SubmissionEntity.
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // Hàm DownloadedSubmission: Đóng gói Resource của tệp bài nộp cùng tên tệp và content type để controller trả
    // response tải xuống.
    public record DownloadedSubmission(Resource resource, String fileName, String contentType) {}
}

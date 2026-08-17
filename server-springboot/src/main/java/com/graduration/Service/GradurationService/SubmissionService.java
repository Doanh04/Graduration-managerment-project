package com.graduration.Service.GradurationService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
    FileStorageService fileStorageService;
    SubmissionMapper submissionMapper;
    CommentService commentService;

    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Transactional
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

        FileStorageService.StoredFile stored = fileStorageService.store(
                file, milestone.getDefensePeriod().getID_Defense(), teamId, milestoneId, extension);
        registerRollbackCleanup(stored.relativePath());
        String originalName = safeOriginalName(file.getOriginalFilename());
        SubmistionEntity submission = SubmistionEntity.builder()
                .filePath(stored.relativePath())
                .fileName(originalName)
                .storedFileName(stored.storedName())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .checksum(stored.checksum())
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
    public SubmissionResponse getSubmission(Long submissionId) {
        SubmistionEntity submission = findSubmission(submissionId);
        requireReadAccess(submission);
        return submissionMapper.toResponse(submission);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
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
    public DownloadedSubmission download(Long submissionId) {
        SubmistionEntity submission = findSubmission(submissionId);
        requireReadAccess(submission);
        return new DownloadedSubmission(
                fileStorageService.load(submission.getFilePath()),
                submission.getFileName(),
                submission.getContentType());
    }

    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Transactional
    public SubmissionResponse withdraw(Long submissionId) {
        SubmistionEntity submission = findSubmission(submissionId);
        StudentEntity student = currentStudent();
        requireTeamMember(submission.getTeam(), student);
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
    public SubmissionResponse startReview(Long submissionId) {
        SubmistionEntity submission = findSubmission(submissionId);
        requireReviewAccess(submission);
        requireStatus(submission, SubmissionStatusConstain.SUBMITTED);
        submission.setStatus(SubmissionStatusConstain.UNDER_REVIEW);
        return submissionMapper.toResponse(submissionRepository.save(submission));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional
    public SubmissionResponse requestRevision(Long submissionId, String comment) {
        return reviewWithComment(
                submissionId,
                comment,
                SubmissionStatusConstain.REVISION_REQUIRED,
                CommentTypeConstain.REVISION_REQUEST);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional
    public SubmissionResponse approve(Long submissionId, String comment) {
        return reviewWithComment(
                submissionId, comment, SubmissionStatusConstain.APPROVED, CommentTypeConstain.APPROVAL);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional
    public SubmissionResponse reject(Long submissionId, String comment) {
        return reviewWithComment(
                submissionId, comment, SubmissionStatusConstain.REJECTED, CommentTypeConstain.REJECTION);
    }

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

    private void validateFilePresent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.SUBMISSION_FILE_REQUIRED);
        }
    }

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

    private void requireSameDefensePeriod(TeamEntity team, StudentEntity student, MilesStoneEntity milestone) {
        Long periodId = milestone.getDefensePeriod().getID_Defense();
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

    private void requireReadAccess(SubmistionEntity submission) {
        if (isManager() || isTeamMember(submission.getTeam()) || isAssignedSupervisor(submission.getTeam())) {
            return;
        }
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    private void requireTeamReadAccess(TeamEntity team) {
        if (isManager() || isTeamMember(team) || isAssignedSupervisor(team)) {
            return;
        }
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    private void requireReviewAccess(SubmistionEntity submission) {
        if (isManager() || isAssignedSupervisor(submission.getTeam())) {
            return;
        }
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

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

    private boolean isTeamMember(TeamEntity team) {
        String userId = currentAuthentication().getName();
        return team.getStudentEntities().stream()
                .anyMatch(student -> student.getUserEntity() != null
                        && userId.equals(student.getUserEntity().getUserId()));
    }

    private void requireTeamMember(TeamEntity team, StudentEntity student) {
        if (team.getStudentEntities().stream()
                .noneMatch(member -> member.getIdStudent().equals(student.getIdStudent()))) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    private StudentEntity currentStudent() {
        return studentRepository
                .findByUserEntity_UserId(currentAuthentication().getName())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));
    }

    private boolean isManager() {
        return hasAuthority("ROLE_ADMIN") || hasAuthority("ROLE_FACULTY");
    }

    private boolean hasAuthority(String authority) {
        return currentAuthentication().getAuthorities().stream()
                .anyMatch(item -> item.getAuthority().equals(authority));
    }

    private Authentication currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication;
    }

    private MilesStoneEntity findMilestone(Long milestoneId) {
        return milestoneRepository
                .findById(milestoneId)
                .orElseThrow(() -> new AppException(ErrorCode.MILESTONE_NOT_FOUND));
    }

    private TeamEntity findTeam(Long teamId) {
        return teamRepository
                .findWithDetailsByIdTeam(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_FOUND));
    }

    private SubmistionEntity findSubmission(Long submissionId) {
        return submissionRepository
                .findById(submissionId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));
    }

    private void requireStatus(SubmistionEntity submission, SubmissionStatusConstain status) {
        if (submission.getStatus() != status) {
            throw new AppException(ErrorCode.SUBMISSION_OPERATION_NOT_ALLOWED);
        }
    }

    private void validateComment(String comment) {
        if (comment == null || comment.isBlank()) {
            throw new AppException(ErrorCode.SUBMISSION_COMMENT_NOT_BLANK);
        }
    }

    private String safeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "submission";
        }
        String normalized = originalName.replace('\\', '/');
        int separator = normalized.lastIndexOf('/');
        String fileName = separator < 0 ? normalized : normalized.substring(separator + 1);
        return fileName.isBlank() ? "submission" : fileName;
    }

    private String extension(String originalName) {
        String safeName = safeOriginalName(originalName);
        int index = safeName.lastIndexOf('.');
        return index < 0 ? "" : safeName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void registerRollbackCleanup(String relativePath) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    fileStorageService.deleteQuietly(relativePath);
                }
            }
        });
    }

    public record DownloadedSubmission(Resource resource, String fileName, String contentType) {}
}

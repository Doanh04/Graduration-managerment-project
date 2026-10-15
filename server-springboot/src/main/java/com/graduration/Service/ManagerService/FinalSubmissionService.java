package com.graduration.Service.ManagerService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.graduration.Constain.*;
import com.graduration.Repository.*;
import com.graduration.entity.*;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.Service.NotificationService.NotificationRecipientService;
import com.graduration.Service.NotificationService.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinalSubmissionService {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final FinalSubmissionRepository submissions;
    private final TopicRepository topics;
    private final ScoreRepository scores;
    private final CommitteeMemberRepository committeeMembers;
    private final GraduationEnrollmentRepository enrollments;
    private final DefensePeriodRepository defensePeriods;
    private final NotificationService notificationService;
    private final NotificationRecipientService notificationRecipientService;

    public record Version(
            Long id,
            int revision,
            String fileName,
            String contentType,
            String sourceUrl,
            String productUrl,
            String note,
            FinalSubmissionStatusConstain status,
            LocalDateTime submittedAt,
            String reviewComment,
            String reviewedBy,
            LocalDateTime reviewedAt) {}

    public record Overview(
            Long topicId,
            String topicTitle,
            String teamName,
            Long defensePeriodId,
            String periodName,
            String academicYear,
            LocalDateTime deadline,
            String status,
            boolean scoreCompleted,
            boolean scorePublished,
            boolean canSubmit,
            boolean canReview,
            List<Version> versions) {}

    public record Decision(Long submissionId, Boolean approved, String comment) {}
    public record FileData(byte[] bytes, String name, String type) {}
    public record BackupArchive(byte[] bytes, String fileName, int fileCount) {}

    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Transactional(readOnly = true)
    public List<Overview> mine() {
        return topics.findFinalSubmissionTopicsForStudent(userId()).stream()
                .map(this::overview)
                .toList();
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public List<Overview> adminList() {
        return topics.findFinalSubmissionTopicsForAdmin().stream()
                .map(this::overview)
                .toList();
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Overview get(Long topicId) {
        TopicEntity topic = topic(topicId);
        requireRead(topic);
        return overview(topic);
    }

    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Transactional
    public Overview submit(
            Long topicId,
            MultipartFile file,
            String sourceUrl,
            String productUrl,
            String note) {
        TopicEntity topic = topic(topicId);
        requireStudent(topic);
        if (!scoreCompleted(topic)) throw new AppException(ErrorCode.SCORE_OPERATION_NOT_ALLOWED);
        LocalDateTime deadline = deadline(topic);
        if (now().isAfter(deadline)) throw new AppException(ErrorCode.DEFENSE_REPORT_CLOSED);

        FinalSubmissionEntity latest = submissions.findFirstByTopic_IdTopicOrderByRevisionDesc(topicId).orElse(null);
        if (latest != null && (latest.getStatus() == FinalSubmissionStatusConstain.PENDING
                || latest.getStatus() == FinalSubmissionStatusConstain.APPROVED)) {
            throw new AppException(ErrorCode.DEFENSE_REPORT_OPERATION_INVALID);
        }
        validateFile(file);
        String normalizedSource = url(sourceUrl);
        String normalizedProduct = url(productUrl);
        if (normalizedSource == null && normalizedProduct == null) {
            throw new AppException(ErrorCode.DEFENSE_REPORT_LINK_REQUIRED);
        }
        try {
            FinalSubmissionEntity entity = FinalSubmissionEntity.builder()
                    .topic(topic)
                    .revision(latest == null ? 1 : latest.getRevision() + 1)
                    .fileName(safeName(file.getOriginalFilename()))
                    .contentType(contentType(file))
                    .fileData(file.getBytes())
                    .sourceUrl(normalizedSource)
                    .productUrl(normalizedProduct)
                    .note(trim(note))
                    .status(FinalSubmissionStatusConstain.PENDING)
                    .submittedBy(userId())
                    .submittedAt(now())
                    .build();
            FinalSubmissionEntity saved = submissions.save(entity);
            notificationService.notifyRoles(
                    java.util.Set.of(RoleConstain.ADMIN, RoleConstain.FACULTY),
                    "Nhóm đã nộp báo cáo bản cuối",
                    topic.getTeam().getNameTeam() + " đã nộp báo cáo bản cuối cho đề tài \""
                            + topic.getTitle() + "\" (lần " + saved.getRevision() + ").",
                    NotificationTypeConstain.FINAL_REPORT_SUBMITTED,
                    "FINAL_SUBMISSION",
                    saved.getId());
            return overview(topic);
        } catch (IOException exception) {
            throw new AppException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    public Overview review(Long topicId, Decision decision) {
        TopicEntity topic = topic(topicId);
        if (decision == null || decision.submissionId() == null || decision.approved() == null) {
            throw new AppException(ErrorCode.DEFENSE_REPORT_OPERATION_INVALID);
        }
        FinalSubmissionEntity entity = submissions.findById(decision.submissionId())
                .filter(item -> topicId.equals(item.getTopic().getIdTopic()))
                .orElseThrow(() -> new AppException(ErrorCode.DEFENSE_REPORT_OPERATION_INVALID));
        FinalSubmissionEntity latest = submissions.findFirstByTopic_IdTopicOrderByRevisionDesc(topicId)
                .orElseThrow(() -> new AppException(ErrorCode.DEFENSE_REPORT_OPERATION_INVALID));
        if (!latest.getId().equals(entity.getId()) || entity.getStatus() != FinalSubmissionStatusConstain.PENDING) {
            throw new AppException(ErrorCode.DEFENSE_REPORT_OPERATION_INVALID);
        }
        if (Boolean.TRUE.equals(decision.approved()) && !scoreCompleted(topic)) {
            throw new AppException(ErrorCode.SCORE_OPERATION_NOT_ALLOWED);
        }
        if (Boolean.FALSE.equals(decision.approved()) && trim(decision.comment()) == null) {
            throw new AppException(ErrorCode.REVIEW_COMMENT_NOT_BLANK);
        }
        entity.setStatus(Boolean.TRUE.equals(decision.approved())
                ? FinalSubmissionStatusConstain.APPROVED
                : FinalSubmissionStatusConstain.REVISION_REQUIRED);
        entity.setReviewComment(trim(decision.comment()));
        entity.setReviewedBy(userId());
        entity.setReviewedAt(now());
        submissions.save(entity);
        if (Boolean.TRUE.equals(decision.approved())) {
            publish(topic);
            notificationService.notifyUsers(
                    notificationRecipientService.studentUserIdsForTopic(topic.getIdTopic()),
                    "Báo cáo bản cuối đã được duyệt",
                    "Báo cáo bản cuối của đề tài \"" + topic.getTitle()
                            + "\" đã được duyệt và điểm đã được công bố.",
                    NotificationTypeConstain.SCORE_PUBLISHED,
                    "FINAL_SUBMISSION",
                    entity.getId());
        } else {
            notificationService.notifyUsers(
                    notificationRecipientService.studentUserIdsForTopic(topic.getIdTopic()),
                    "Báo cáo bản cuối cần nộp lại",
                    "Báo cáo bản cuối của đề tài \"" + topic.getTitle() + "\" cần nộp lại. Lý do: "
                            + entity.getReviewComment(),
                    NotificationTypeConstain.FINAL_REPORT_REJECTED,
                    "FINAL_SUBMISSION",
                    entity.getId());
        }
        return overview(topic);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public FileData file(Long submissionId) {
        FinalSubmissionEntity entity = submissions.findById(submissionId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));
        requireRead(entity.getTopic());
        return new FileData(entity.getFileData(), entity.getFileName(), entity.getContentType());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional(readOnly = true)
    public BackupArchive backup(Long defensePeriodId) {
        DefensePeriodEntity period = defensePeriods.findById(defensePeriodId)
                .orElseThrow(() -> new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND));
        List<FinalSubmissionEntity> rows = submissions.findAllForBackup(defensePeriodId);
        if (rows.isEmpty()) {
            throw new AppException(ErrorCode.FINAL_SUBMISSION_BACKUP_EMPTY);
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            Set<String> entryNames = new HashSet<>();
            for (FinalSubmissionEntity row : rows) {
                String folder = archiveSegment(row.getTopic().getTeam() == null
                        ? null : row.getTopic().getTeam().getNameTeam(), "nhom")
                        + " - " + archiveSegment(row.getTopic().getTitle(), "de-tai-" + row.getTopic().getIdTopic());
                String reportName = uniqueEntryName(entryNames, folder + "/phien-ban-" + row.getRevision()
                        + "_" + archiveSegment(row.getFileName(), "bao-cao"));
                writeZipEntry(zip, reportName, row.getFileData());

                String information = "Đề tài: " + nullSafe(row.getTopic().getTitle()) + System.lineSeparator()
                        + "Nhóm: " + nullSafe(row.getTopic().getTeam() == null
                                ? null : row.getTopic().getTeam().getNameTeam()) + System.lineSeparator()
                        + "Phiên bản: " + row.getRevision() + System.lineSeparator()
                        + "Trạng thái: " + row.getStatus() + System.lineSeparator()
                        + "Thời gian nộp: " + nullSafe(row.getSubmittedAt()) + System.lineSeparator()
                        + "Source code: " + nullSafe(row.getSourceUrl()) + System.lineSeparator()
                        + "Sản phẩm/demo: " + nullSafe(row.getProductUrl()) + System.lineSeparator()
                        + "Ghi chú: " + nullSafe(row.getNote()) + System.lineSeparator();
                writeZipEntry(zip, uniqueEntryName(entryNames,
                        folder + "/phien-ban-" + row.getRevision() + "_thong-tin.txt"),
                        information.getBytes(StandardCharsets.UTF_8));
            }
            zip.finish();
            return new BackupArchive(output.toByteArray(),
                    archiveSegment(period.getPeriodName(), "dot-bao-ve") + ".zip", rows.size());
        } catch (IOException exception) {
            throw new AppException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    @Transactional
    public void settleDeadline(Long topicId) {
        TopicEntity topic = topic(topicId);
        if (now().isBefore(deadline(topic)) || !scoreCompleted(topic) || scorePublished(topic)) return;
        if (submissions.existsByTopic_IdTopicAndStatus(topicId, FinalSubmissionStatusConstain.APPROVED)) {
            publish(topic);
            return;
        }
        FinalSubmissionEntity latest = submissions.findFirstByTopic_IdTopicOrderByRevisionDesc(topicId).orElse(null);
        // Sinh viên đã nộp đúng hạn thì không bị 0 chỉ vì Admin chưa kịp duyệt.
        if (latest != null && latest.getStatus() == FinalSubmissionStatusConstain.PENDING
                && latest.getSubmittedAt() != null && !latest.getSubmittedAt().isAfter(deadline(topic))) {
            return;
        }
        LocalDateTime settledAt = now();
        for (StudentEntity student : topic.getTeam().getStudentEntities()) {
            GraduationEnrollmentEntity enrollment = enrollment(topic, student);
            if (enrollment != null && enrollment.getStatus() == EnrollmentStatusConstain.WITHDRAWN) continue;
            if (!scores.existsByAutomaticFinalTopicIdAndStudent_IdStudent(topicId, student.getIdStudent())) {
                scores.save(ScoreEntity.builder()
                        .student(student)
                        .topic(topic)
                        .score(BigDecimal.ZERO.setScale(2))
                        .scoreType(ScoreTypeConstain.FINAL)
                        .status(ScoreStatusConstain.LOCKED)
                        .automaticFinalTopicId(topicId)
                        .comment("Quá hạn nộp bản báo cáo final hoặc bản final chưa được Admin duyệt")
                        .submittedAt(settledAt)
                        .publishedAt(settledAt)
                        .build());
            }
            if (enrollment != null) {
                enrollment.setStatus(EnrollmentStatusConstain.FAILED);
                enrollment.setCompletedAt(settledAt);
                enrollment.setNote("Quá hạn nộp/duyệt bản final");
                enrollments.save(enrollment);
            }
        }
        notificationService.notifyUsersOnce(
                notificationRecipientService.studentUserIdsForTopic(topic.getIdTopic()),
                "Quá hạn nộp báo cáo bản cuối",
                "Nhóm chưa hoàn tất báo cáo bản cuối đúng hạn cho đề tài \"" + topic.getTitle()
                        + "\". Hệ thống đã ghi nhận và công bố 0 điểm.",
                NotificationTypeConstain.DEADLINE_EXPIRED,
                "FINAL_SUBMISSION_DEADLINE",
                topic.getIdTopic());
    }

    private void publish(TopicEntity topic) {
        LocalDateTime publishedAt = now();
        for (StudentEntity student : topic.getTeam().getStudentEntities()) {
            List<ScoreEntity> rows = scores.findCommitteeScores(student.getIdStudent(), topic.getIdTopic());
            rows.stream()
                    .filter(row -> row.getStatus() == ScoreStatusConstain.LOCKED)
                    .forEach(row -> row.setPublishedAt(publishedAt));
            scores.saveAll(rows);
            GraduationEnrollmentEntity enrollment = enrollment(topic, student);
            if (enrollment != null && enrollment.getStatus() != EnrollmentStatusConstain.WITHDRAWN) {
                enrollment.setStatus(EnrollmentStatusConstain.COMPLETED);
                enrollment.setCompletedAt(publishedAt);
                enrollments.save(enrollment);
            }
        }
    }

    private Overview overview(TopicEntity topic) {
        List<FinalSubmissionEntity> rows = submissions.findByTopic_IdTopicOrderByRevisionDesc(topic.getIdTopic());
        boolean completed = scoreCompleted(topic);
        boolean published = scorePublished(topic);
        LocalDateTime deadline = deadline(topic);
        FinalSubmissionEntity latest = rows.isEmpty() ? null : rows.get(0);
        String status = published ? "PUBLISHED"
                : latest != null ? latest.getStatus().name()
                : now().isAfter(deadline) && completed ? "EXPIRED"
                : completed ? "NOT_SUBMITTED" : "WAITING_SCORE";
        boolean student = hasRole("ROLE_STUDENT");
        return new Overview(
                topic.getIdTopic(), topic.getTitle(), topic.getTeam().getNameTeam(),
                topic.getDefensePeriod().getID_Defense(),
                topic.getDefensePeriod().getPeriodName(),
                topic.getDefensePeriod().getAcademicYear() == null ? null
                        : topic.getDefensePeriod().getAcademicYear().getAcademicYear(),
                deadline, status, completed, published,
                student && completed && !published && !now().isAfter(deadline)
                        && (latest == null || latest.getStatus() == FinalSubmissionStatusConstain.REVISION_REQUIRED),
                !student && latest != null && latest.getStatus() == FinalSubmissionStatusConstain.PENDING,
                rows.stream().map(this::version).toList());
    }

    private Version version(FinalSubmissionEntity item) {
        return new Version(item.getId(), item.getRevision(), item.getFileName(), item.getContentType(),
                item.getSourceUrl(), item.getProductUrl(), item.getNote(), item.getStatus(), item.getSubmittedAt(),
                item.getReviewComment(), item.getReviewedBy(), item.getReviewedAt());
    }

    private boolean scoreCompleted(TopicEntity topic) {
        if (topic.getTeam() == null || topic.getDefenseSchedule() == null
                || topic.getDefenseSchedule().getDefenseCommittees() == null) return false;
        Set<String> scoringLecturerIds = committeeMembers
                .findByDefenseCommittees_IdComitteesAndStatusOrderByRoleAsc(
                        topic.getDefenseSchedule().getDefenseCommittees().getIdComittees(),
                        CommitteeMemberStatusConstain.ACTIVE)
                .stream()
                .filter(member -> member.getRole() == CommitteeMemberRoleConstain.CHAIRPERSON
                        || member.getRole() == CommitteeMemberRoleConstain.REVIEWER)
                .map(member -> member.getLecture().getLectureId())
                .collect(java.util.stream.Collectors.toSet());
        if (scoringLecturerIds.isEmpty()) return false;
        for (StudentEntity student : topic.getTeam().getStudentEntities()) {
            GraduationEnrollmentEntity enrollment = enrollment(topic, student);
            if (enrollment != null && enrollment.getStatus() == EnrollmentStatusConstain.WITHDRAWN) continue;
            List<ScoreEntity> rows = scores.findCommitteeScores(student.getIdStudent(), topic.getIdTopic()).stream()
                    .filter(row -> row.getLecture() != null
                            && scoringLecturerIds.contains(row.getLecture().getLectureId()))
                    .toList();
            if (rows.size() != scoringLecturerIds.size()
                    || rows.stream().anyMatch(row -> row.getStatus() != ScoreStatusConstain.LOCKED)) {
                return false;
            }
        }
        return true;
    }

    private boolean scorePublished(TopicEntity topic) {
        return topic.getTeam().getStudentEntities().stream().anyMatch(student ->
                scores.findByStudentAndDefensePeriod(student.getIdStudent(), topic.getDefensePeriod().getID_Defense())
                        .stream().anyMatch(score -> topic.getIdTopic().equals(score.getTopic().getIdTopic())
                                && score.getPublishedAt() != null));
    }

    private GraduationEnrollmentEntity enrollment(TopicEntity topic, StudentEntity student) {
        return enrollments.findByStudent_IdStudentAndDefensePeriod_ID_Defense(
                student.getIdStudent(), topic.getDefensePeriod().getID_Defense()).orElse(null);
    }

    private TopicEntity topic(Long id) {
        return topics.findById(id).orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
    }

    private LocalDateTime deadline(TopicEntity topic) {
        LocalDateTime configured = topic.getDefensePeriod().getFinalSubmissionDeadline();
        return configured != null ? configured : topic.getDefensePeriod().getEndDate().atTime(23, 59, 59);
    }

    private void requireRead(TopicEntity topic) {
        if (hasRole("ROLE_ADMIN") || hasRole("ROLE_FACULTY")) return;
        requireStudent(topic);
    }

    private void requireStudent(TopicEntity topic) {
        boolean member = topic.getTeam() != null && topic.getTeam().getStudentEntities().stream()
                .anyMatch(student -> student.getUserEntity() != null
                        && userId().equals(student.getUserEntity().getUserId()));
        if (!member) throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw new AppException(ErrorCode.DEFENSE_REPORT_FILE_INVALID);
        }
        String name = safeName(file.getOriginalFilename()).toLowerCase();
        if (!(name.endsWith(".pdf") || name.endsWith(".docx"))) {
            throw new AppException(ErrorCode.DEFENSE_REPORT_FILE_INVALID);
        }
    }

    private String contentType(MultipartFile file) {
        return safeName(file.getOriginalFilename()).toLowerCase().endsWith(".pdf")
                ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }

    private String url(String value) {
        String normalized = trim(value);
        if (normalized == null) return null;
        try {
            URI uri = URI.create(normalized);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) throw new IllegalArgumentException();
            return normalized;
        } catch (RuntimeException exception) {
            throw new AppException(ErrorCode.DEFENSE_REPORT_LINK_INVALID);
        }
    }

    private String safeName(String name) {
        if (name == null || name.isBlank()) return "final-report";
        return name.replace('\\', '/').substring(name.replace('\\', '/').lastIndexOf('/') + 1);
    }

    private String archiveSegment(String value, String fallback) {
        String normalized = value == null ? "" : value.trim()
                .replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_")
                .replaceAll("\\.{2,}", "_");
        normalized = normalized.replaceAll("^[. ]+|[. ]+$", "");
        return normalized.isBlank() ? fallback : normalized;
    }

    private String uniqueEntryName(Set<String> names, String requested) {
        String candidate = requested;
        int counter = 2;
        int extensionIndex = requested.lastIndexOf('.');
        String base = extensionIndex > requested.lastIndexOf('/') ? requested.substring(0, extensionIndex) : requested;
        String extension = extensionIndex > requested.lastIndexOf('/') ? requested.substring(extensionIndex) : "";
        while (!names.add(candidate)) candidate = base + "_" + counter++ + extension;
        return candidate;
    }

    private void writeZipEntry(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content == null ? new byte[0] : content);
        zip.closeEntry();
    }

    private String nullSafe(Object value) { return value == null ? "—" : value.toString(); }

    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private LocalDateTime now() { return LocalDateTime.now(ZONE); }
    private String userId() { return SecurityContextHolder.getContext().getAuthentication().getName(); }
    private boolean hasRole(String role) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}

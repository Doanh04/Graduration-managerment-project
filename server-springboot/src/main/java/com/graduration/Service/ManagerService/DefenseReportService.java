package com.graduration.Service.ManagerService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.zip.ZipInputStream;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.graduration.Constain.*;
import com.graduration.Repository.*;
import com.graduration.entity.*;
import com.graduration.exception.*;
import com.graduration.Service.NotificationService.NotificationRecipientService;
import com.graduration.Service.NotificationService.NotificationService;
import lombok.RequiredArgsConstructor;

/** Hồ sơ tiền kiểm gắn với phân công phản biện, được hoàn tất trước khi xếp lịch bảo vệ. */
@Service @RequiredArgsConstructor
public class DefenseReportService {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ReviewAssignmentRepository assignments;
    private final DefenseReportRepository reports;
    private final ScoreRepository scores;
    private final GraduationEnrollmentRepository enrollments;
    private final UserRepository users;
    private final EntityManager entityManager;
    private final ReviewAssignmentProvisioningService reviewProvisioning;
    private final NotificationService notificationService;
    private final NotificationRecipientService notificationRecipientService;

    public record Version(Long id, int revision, String fileName, String contentType, String sourceUrl,
            String productUrl, String note, LocalDateTime submittedAt, String status, String reviewComment,
            String reviewerName, LocalDateTime reviewedAt) {}
    public record Overview(Long assignmentId, String topicTitle, String teamName, String periodName,
            String academicYear, String committeeName, LocalDateTime deadline, String status, String failureReason,
            boolean canSubmit, boolean canReview, boolean canScore, String assignmentStatus, List<Version> versions) {}
    public record Decision(Long reportId, Boolean approved, Boolean terminal, String comment) {}
    public record FileData(byte[] bytes, String name, String type) {}

    @PreAuthorize("hasAuthority('ROLE_STUDENT')") @Transactional
    public List<Overview> mine() {
        reviewProvisioning.provisionForStudentUser(userId());
        return assignments.findStudentReportAssignments(userId()).stream()
                .map(item -> getInternal(item.getReviewAssignmentId())).toList();
    }

    @PreAuthorize("isAuthenticated()") @Transactional
    public Overview get(Long assignmentId) { return getInternal(assignmentId); }

    private Overview getInternal(Long assignmentId) {
        var assignment = lock(assignmentId);
        requireRead(assignment);
        settle(assignment);
        return overview(assignment);
    }

    @PreAuthorize("hasAuthority('ROLE_STUDENT')") @Transactional
    public Overview submit(Long assignmentId, MultipartFile file, String sourceUrl, String productUrl, String note) {
        var assignment = lock(assignmentId);
        requireStudent(assignment); requireOpen(assignment);
        var versions = versions(assignment);
        if (!versions.isEmpty() && "PENDING".equals(versions.get(0).getStatus()))
            throw new AppException(ErrorCode.DEFENSE_REPORT_OPERATION_INVALID);
        String source = link(sourceUrl), product = link(productUrl);
        if (source == null && product == null) throw new AppException(ErrorCode.DEFENSE_REPORT_LINK_REQUIRED);
        if (note != null && note.length() > 4000) throw new AppException(ErrorCode.INVALID_KEY);
        if (file == null || file.isEmpty() || file.getSize() > 10L * 1024 * 1024)
            throw new AppException(ErrorCode.DEFENSE_REPORT_FILE_INVALID);
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\r\\n]", "");
        if (name.isBlank() || name.length() > 200) throw new AppException(ErrorCode.DEFENSE_REPORT_FILE_INVALID);
        try {
            byte[] data = file.getBytes();
            boolean pdf = name.toLowerCase(Locale.ROOT).endsWith(".pdf") && data.length >= 5
                    && new String(data, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-");
            boolean docx = name.toLowerCase(Locale.ROOT).endsWith(".docx") && isDocx(data);
            if (!pdf && !docx) throw new AppException(ErrorCode.DEFENSE_REPORT_FILE_INVALID);
            requireOpen(assignment);
            var report = new DefenseReportEntity();
            report.setReviewAssignment(assignment);
            report.setRevision(versions.isEmpty() ? 1 : versions.get(0).getRevision() + 1);
            report.setFileName(name);
            report.setContentType(pdf ? "application/pdf" : "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            report.setFileData(data); report.setSourceUrl(source); report.setProductUrl(product); report.setNote(note);
            report.setSubmittedBy(userId()); report.setSubmittedAt(now()); report.setStatus("PENDING");
            reports.saveAndFlush(report);
            assignment.setApprovedReportId(null); assignment.setStatus(ReviewAssignmentStatusConstain.IN_PROGRESS);
            assignment.setRecommendation(null); assignment.setReportFailureReason(null);
            return overview(assignment);
        } catch (IOException ex) { throw new AppException(ErrorCode.FILE_STORAGE_ERROR); }
    }

    @PreAuthorize("hasAnyAuthority('ROLE_REVIEWER', 'ROLE_SUPERVISOR', 'ROLE_ADMIN', 'ROLE_FACULTY')") @Transactional
    public Overview review(Long assignmentId, Decision decision) {
        var assignment = lock(assignmentId);
        if (!isReviewer(assignment, userId())) throw new AppException(ErrorCode.ACCESS_DENIED);
        requireOpen(assignment);
        var all = versions(assignment);
        if (decision == null || decision.approved() == null || all.isEmpty()
                || !Objects.equals(all.get(0).getId(), decision.reportId()) || !"PENDING".equals(all.get(0).getStatus()))
            throw new AppException(ErrorCode.DEFENSE_REPORT_OPERATION_INVALID);
        boolean terminal = Boolean.TRUE.equals(decision.terminal());
        if (!decision.approved() && (decision.comment() == null || decision.comment().isBlank()))
            throw new AppException(ErrorCode.REVIEW_COMMENT_NOT_BLANK);
        if (decision.comment() != null && decision.comment().length() > 4000) throw new AppException(ErrorCode.INVALID_KEY);
        requireOpen(assignment);
        var report = all.get(0);
        UserEntity reviewerUser = currentUser();
        report.setStatus(decision.approved() ? "APPROVED" : terminal ? "REJECTED" : "REVISION_REQUIRED");
        report.setReviewComment(decision.comment()); report.setReviewedBy(userId()); report.setReviewedAt(now());
        report.setReviewerName(reviewerUser != null && reviewerUser.getLecture() != null
                ? reviewerUser.getLecture().getFullNameLecture()
                : assignment.getLecture().getFullNameLecture());
        assignment.setReviewComment(decision.comment()); assignment.setSubmittedAt(now()); assignment.setReviewedAt(now());
        assignment.setReviewedBy(reviewerUser);
        if (decision.approved()) {
            assignment.setApprovedReportId(report.getId());
            assignment.setRecommendation(ReviewRecommendationConstain.ELIGIBLE_FOR_DEFENSE);
            assignment.setStatus(ReviewAssignmentStatusConstain.APPROVED);
            assignment.setReportCheckedAt(now());
        } else if (terminal) {
            assignment.setRecommendation(ReviewRecommendationConstain.NOT_ELIGIBLE_FOR_DEFENSE);
            fail(assignment, "Giảng viên phản biện kết luận hồ sơ không đủ điều kiện bảo vệ");
        } else {
            assignment.setApprovedReportId(null);
            assignment.setRecommendation(ReviewRecommendationConstain.REVISION_REQUIRED);
            assignment.setStatus(ReviewAssignmentStatusConstain.REVISION_REQUIRED);
        }
        return overview(assignment);
    }

    @PreAuthorize("isAuthenticated()") @Transactional(readOnly = true)
    public FileData file(Long reportId) {
        var report = reports.findById(reportId).orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));
        if (report.getReviewAssignment() == null) throw new AppException(ErrorCode.ACCESS_DENIED);
        requireRead(report.getReviewAssignment());
        return new FileData(report.getFileData(), report.getFileName(), report.getContentType());
    }

    @Transactional public void settleDeadline(Long assignmentId) { settle(lock(assignmentId)); }

    public void requireEligible(DefenseSchedulesEntity schedule) {
        requireReviewApproved(schedule);
        if (!Boolean.TRUE.equals(schedule.getReportRequired())) return;
        LocalDateTime start = LocalDateTime.of(schedule.getDefenseDate(), schedule.getStartTime());
        if (now().isBefore(start)) throw new AppException(ErrorCode.DEFENSE_REPORT_NOT_STARTED);
    }

    public void requireReviewApproved(DefenseSchedulesEntity schedule) {
        if (schedule == null) throw new AppException(ErrorCode.DEFENSE_SCHEDULE_NOT_FOUND);
        if (!Boolean.TRUE.equals(schedule.getReportRequired())) return;
        if (!hasApprovedReview(
                schedule.getTopic().getIdTopic(), schedule.getDefenseCommittees().getIdComittees()))
            throw new AppException(ErrorCode.DEFENSE_REPORT_NOT_APPROVED);
    }

    public void requireScheduleEditable(DefenseSchedulesEntity schedule) {}
    public void invalidateApproval(DefenseSchedulesEntity schedule) {}

    @Transactional(readOnly = true)
    public boolean hasApprovedReview(Long topicId, Long committeeId) {
        // APPROVED is the final decision recorded by the committee. Changing the
        // committee's current reviewer later must not revoke that historical decision.
        return assignments
                .findFirstByTopic_IdTopicAndDefenseCommittee_IdComitteesAndStatusOrderByReviewedAtDesc(
                        topicId, committeeId, ReviewAssignmentStatusConstain.APPROVED)
                .isPresent();
    }

    private void settle(ReviewAssignmentEntity assignment) {
        if (assignment.getReportCheckedAt() != null
                || assignment.getStatus() == ReviewAssignmentStatusConstain.APPROVED
                || assignment.getStatus() == ReviewAssignmentStatusConstain.NOT_ELIGIBLE
                || assignment.getStatus() == ReviewAssignmentStatusConstain.CANCELLED
                || assignment.getDeadline() == null || now().isBefore(assignment.getDeadline())) return;
        var all = versions(assignment);
        String reason = all.isEmpty() ? "Chưa nộp hồ sơ trước hạn phản biện"
                : "REVISION_REQUIRED".equals(all.get(0).getStatus()) ? "Chưa nộp lại hồ sơ theo yêu cầu phản biện"
                : "Hồ sơ chưa được giảng viên phản biện đồng ý trước hạn";
        fail(assignment, reason);
    }

    private void fail(ReviewAssignmentEntity assignment, String reason) {
        if (assignment.getReportCheckedAt() != null) return;
        var topic = assignment.getTopic();
        if (topic.getTeam() != null) for (var student : topic.getTeam().getStudentEntities()) {
            var enrollment = enrollments.findByStudent_IdStudentAndDefensePeriod_ID_Defense(
                    student.getIdStudent(), topic.getDefensePeriod().getID_Defense()).orElse(null);
            if (enrollment != null && enrollment.getStatus() == EnrollmentStatusConstain.WITHDRAWN) continue;
            boolean existing = scores.findByStudentAndDefensePeriod(student.getIdStudent(), topic.getDefensePeriod().getID_Defense())
                    .stream().anyMatch(score -> topic.getIdTopic().equals(score.getTopic().getIdTopic()));
            if (!existing) scores.save(ScoreEntity.builder().student(student).topic(topic)
                    .score(BigDecimal.ZERO.setScale(2)).scoreType(ScoreTypeConstain.FINAL)
                    .status(ScoreStatusConstain.LOCKED).automaticReviewAssignmentId(assignment.getReviewAssignmentId())
                    .comment("Tự động: " + reason).submittedAt(now()).publishedAt(now()).build());
            if (enrollment != null) { enrollment.setStatus(EnrollmentStatusConstain.FAILED); enrollment.setCompletedAt(now()); }
        }
        assignment.setApprovedReportId(null); assignment.setReportFailureReason(reason); assignment.setReportCheckedAt(now());
        assignment.setStatus(ReviewAssignmentStatusConstain.NOT_ELIGIBLE);
        assignment.setRecommendation(ReviewRecommendationConstain.NOT_ELIGIBLE_FOR_DEFENSE);
        assignment.setReviewedAt(now());
        notificationService.notifyUsersOnce(
                notificationRecipientService.studentUserIdsForTopic(topic.getIdTopic()),
                "Không đủ điều kiện bảo vệ",
                "Đề tài \"" + topic.getTitle() + "\" không đủ điều kiện bảo vệ. Lý do: " + reason
                        + ". Điểm được ghi nhận là 0.",
                NotificationTypeConstain.DEFENSE_BANNED,
                "REVIEW_ASSIGNMENT",
                assignment.getReviewAssignmentId());
    }

    private Overview overview(ReviewAssignmentEntity assignment) {
        var all = versions(assignment);
        String state = assignment.getReportFailureReason() != null ? "FAILED"
                : assignment.getStatus() == ReviewAssignmentStatusConstain.APPROVED ? "APPROVED"
                : all.isEmpty() ? "NOT_SUBMITTED" : all.get(0).getStatus();
        boolean open = isOpen(assignment);
        var topic = assignment.getTopic(); var period = topic.getDefensePeriod();
        return new Overview(assignment.getReviewAssignmentId(), topic.getTitle(),
                topic.getTeam() == null ? "" : topic.getTeam().getNameTeam(), period.getPeriodName(),
                period.getAcademicYear().getAcademicYear(), assignment.getDefenseCommittee() == null ? null : assignment.getDefenseCommittee().getComitteesName(),
                assignment.getDeadline(), state, assignment.getReportFailureReason(),
                open && isStudent(assignment) && !"PENDING".equals(state),
                open && isReviewer(assignment, userId()) && "PENDING".equals(state), false,
                assignment.getStatus().name(), all.stream().map(r -> new Version(r.getId(), r.getRevision(), r.getFileName(),
                        r.getContentType(), r.getSourceUrl(), r.getProductUrl(), r.getNote(), r.getSubmittedAt(), r.getStatus(),
                        r.getReviewComment(), r.getReviewerName(), r.getReviewedAt())).toList());
    }

    private boolean isOpen(ReviewAssignmentEntity assignment) {
        return assignment.getReportCheckedAt() == null && assignment.getDeadline() != null && now().isBefore(assignment.getDeadline())
                && assignment.getStatus() != ReviewAssignmentStatusConstain.CANCELLED
                && assignment.getStatus() != ReviewAssignmentStatusConstain.APPROVED
                && assignment.getStatus() != ReviewAssignmentStatusConstain.NOT_ELIGIBLE;
    }
    private boolean isReviewer(ReviewAssignmentEntity assignment, String id) {
        if (id == null || assignment.getDefenseCommittee() == null) return false;
        return assignment.getDefenseCommittee().getComitteesMember().stream().anyMatch(member ->
                member.getStatus() == CommitteeMemberStatusConstain.ACTIVE
                && member.getRole() == CommitteeMemberRoleConstain.REVIEWER
                && member.getLecture() != null
                && member.getLecture().getUser() != null
                && id.equals(member.getLecture().getUser().getUserId()));
    }
    private boolean isStudent(ReviewAssignmentEntity assignment) {
        return assignment.getTopic().getTeam() != null && assignment.getTopic().getTeam().getStudentEntities().stream()
                .anyMatch(st -> st.getUserEntity() != null && userId().equals(st.getUserEntity().getUserId())
                        && enrollments.findByStudent_IdStudentAndDefensePeriod_ID_Defense(st.getIdStudent(), assignment.getTopic().getDefensePeriod().getID_Defense())
                        .map(e -> e.getStatus() != EnrollmentStatusConstain.WITHDRAWN).orElse(true));
    }
    private void requireStudent(ReviewAssignmentEntity assignment) { if (!isStudent(assignment)) throw new AppException(ErrorCode.ACCESS_DENIED); }
    private void requireRead(ReviewAssignmentEntity assignment) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> Set.of("ROLE_ADMIN", "ROLE_FACULTY").contains(a.getAuthority()))) return;
        if (assignment.getTopic().getTeam() != null && assignment.getTopic().getTeam().getStudentEntities().stream()
                .anyMatch(st -> st.getUserEntity() != null && userId().equals(st.getUserEntity().getUserId()))) return;
        if (isActiveCommitteeMember(assignment, userId())) return;
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }
    private boolean isActiveCommitteeMember(ReviewAssignmentEntity assignment, String id) {
        if (id == null || assignment.getDefenseCommittee() == null) return false;
        return assignment.getDefenseCommittee().getComitteesMember().stream().anyMatch(member ->
                member.getStatus() == CommitteeMemberStatusConstain.ACTIVE
                && member.getLecture() != null
                && member.getLecture().getUser() != null
                && id.equals(member.getLecture().getUser().getUserId()));
    }
    private void requireOpen(ReviewAssignmentEntity assignment) {
        if (!isOpen(assignment)) throw new AppException(ErrorCode.DEFENSE_REPORT_CLOSED);
    }
    private ReviewAssignmentEntity lock(Long id) {
        var assignment = assignments.lockForReport(id).orElseThrow(() -> new AppException(ErrorCode.REVIEW_ASSIGNMENT_NOT_FOUND));
        entityManager.refresh(assignment, LockModeType.PESSIMISTIC_WRITE);
        return assignment;
    }
    private List<DefenseReportEntity> versions(ReviewAssignmentEntity assignment) {
        return reports.findByReviewAssignment_ReviewAssignmentIdOrderByRevisionDesc(assignment.getReviewAssignmentId());
    }
    private LocalDateTime now() { return LocalDateTime.now(ZONE); }
    private String userId() { var auth = SecurityContextHolder.getContext().getAuthentication(); return auth == null ? "" : auth.getName(); }
    private UserEntity currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : users.findById(auth.getName()).orElse(null);
    }
    private String link(String value) {
        if (value == null || value.isBlank()) return null;
        try { var uri = URI.create(value.trim()); if (value.length() > 2000 || !Set.of("https", "http").contains(String.valueOf(uri.getScheme()).toLowerCase(Locale.ROOT))
                || uri.getHost() == null || uri.getUserInfo() != null) throw new IllegalArgumentException(); return uri.toASCIIString(); }
        catch (IllegalArgumentException ex) { throw new AppException(ErrorCode.DEFENSE_REPORT_LINK_INVALID); }
    }
    private boolean isDocx(byte[] data) throws IOException {
        try (var zip = new ZipInputStream(new ByteArrayInputStream(data))) {
            long expanded = 0; byte[] buffer = new byte[8192];
            for (int i = 0; i < 1000; i++) { var entry = zip.getNextEntry(); if (entry == null) return false;
                if ("word/document.xml".equals(entry.getName())) return true;
                int count; while ((count = zip.read(buffer)) != -1) { expanded += count; if (expanded > 30L * 1024 * 1024) return false; }
            }
            return false;
        }
    }
}

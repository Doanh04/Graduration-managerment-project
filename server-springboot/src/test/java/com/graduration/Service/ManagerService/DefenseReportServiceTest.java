package com.graduration.Service.ManagerService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.graduration.Constain.*;
import com.graduration.Repository.*;
import com.graduration.entity.*;
import com.graduration.exception.AppException;
import com.graduration.Service.NotificationService.NotificationRecipientService;
import com.graduration.Service.NotificationService.NotificationService;

class DefenseReportServiceTest {
    ReviewAssignmentRepository assignments;
    DefenseReportRepository reports;
    ScoreRepository scores;
    GraduationEnrollmentRepository enrollments;
    UserRepository users;
    ReviewAssignmentProvisioningService reviewProvisioning;
    DefenseReportService service;
    ReviewAssignmentEntity assignment;
    DefenseSchedulesEntity schedule;
    StudentEntity student;
    ComitteesMemberEntity reviewerMember;
    List<DefenseReportEntity> versions;

    @BeforeEach
    void setup() {
        assignments = mock(ReviewAssignmentRepository.class);
        reports = mock(DefenseReportRepository.class);
        scores = mock(ScoreRepository.class);
        enrollments = mock(GraduationEnrollmentRepository.class);
        users = mock(UserRepository.class);
        reviewProvisioning = mock(ReviewAssignmentProvisioningService.class);
        service = new DefenseReportService(
                assignments, reports, scores, enrollments, users, mock(jakarta.persistence.EntityManager.class),
                reviewProvisioning, mock(NotificationService.class), mock(NotificationRecipientService.class));

        UserEntity studentUser = UserEntity.builder().userId("student").build();
        UserEntity reviewerUser = UserEntity.builder().userId("reviewer").build();
        student = StudentEntity.builder().idStudent("student-1").userEntity(studentUser).build();
        var year = new AcademicYearEntity();
        year.setAcademicYear("2026-2027");
        var period = new DefensePeriodEntity();
        period.setID_Defense(1L);
        period.setAcademicYear(year);
        period.setPeriodName("Đợt 1");
        var team = TeamEntity.builder()
                .nameTeam("Nhóm 1")
                .studentEntities(new ArrayList<>(List.of(student)))
                .build();
        var topic = TopicEntity.builder()
                .idTopic(2L)
                .title("Đề tài")
                .team(team)
                .defensePeriod(period)
                .build();
        var reviewer = LectureEntity.builder()
                .lectureId("lecture-1")
                .fullNameLecture("Giảng viên phản biện")
                .user(reviewerUser)
                .build();
        reviewerMember = ComitteesMemberEntity.builder()
                .role(CommitteeMemberRoleConstain.REVIEWER)
                .status(CommitteeMemberStatusConstain.ACTIVE)
                .lecture(reviewer)
                .build();
        var committee = new DefenseCommitteesEntity();
        committee.setIdComittees(4L);
        committee.setComitteesName("Hội đồng 1");
        committee.setComitteesMember(new ArrayList<>(List.of(reviewerMember)));
        assignment = ReviewAssignmentEntity.builder()
                .reviewAssignmentId(3L)
                .topic(topic)
                .lecture(reviewer)
                .defenseCommittee(committee)
                .deadline(LocalDateTime.now(DefenseReportService.ZONE).plusDays(1))
                .status(ReviewAssignmentStatusConstain.ASSIGNED)
                .build();
        schedule = DefenseSchedulesEntity.builder()
                .idDefenseScheduce(5L)
                .topic(topic)
                .defenseCommittees(committee)
                .reportRequired(true)
                .status(DefenseScheduleStatusConstain.PUBLISHED)
                .defenseDate(LocalDate.now(DefenseReportService.ZONE).plusDays(1))
                .startTime(LocalTime.NOON)
                .build();
        versions = new ArrayList<>();

        when(assignments.lockForReport(3L)).thenReturn(Optional.of(assignment));
        when(assignments.findFirstByTopic_IdTopicAndDefenseCommittee_IdComitteesAndStatusOrderByReviewedAtDesc(
                        2L, 4L, ReviewAssignmentStatusConstain.APPROVED))
                .thenAnswer(invocation -> assignment.getStatus() == ReviewAssignmentStatusConstain.APPROVED
                        ? Optional.of(assignment) : Optional.empty());
        when(reports.findByReviewAssignment_ReviewAssignmentIdOrderByRevisionDesc(3L))
                .thenAnswer(invocation -> versions);
        when(reports.saveAndFlush(any())).thenAnswer(invocation -> {
            DefenseReportEntity report = invocation.getArgument(0);
            report.setId((long) versions.size() + 1);
            versions.add(0, report);
            return report;
        });
        when(users.findById("reviewer")).thenReturn(Optional.of(reviewerUser));
        authenticate("student", "ROLE_STUDENT");
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void studentSubmitsBeforeScheduleAndApprovalMakesTopicEligible() {
        submit();
        assertEquals("PENDING", versions.get(0).getStatus());
        assertEquals(ReviewAssignmentStatusConstain.IN_PROGRESS, assignment.getStatus());

        approveLatest();
        assertEquals(ReviewAssignmentStatusConstain.APPROVED, assignment.getStatus());
        assertEquals(ReviewRecommendationConstain.ELIGIBLE_FOR_DEFENSE, assignment.getRecommendation());
        assertTrue(service.hasApprovedReview(2L, 4L));

        assertThrows(AppException.class, () -> service.requireEligible(schedule));
        moveDefenseToPast();
        service.requireEligible(schedule);
        verify(scores, never()).save(any());
    }

    @Test
    void revisionKeepsHistoryAndStudentCanResubmit() {
        submit();
        authenticate("reviewer", "ROLE_REVIEWER");
        service.review(3L, new DefenseReportService.Decision(1L, false, false, "Bổ sung kết quả"));
        assertEquals(ReviewAssignmentStatusConstain.REVISION_REQUIRED, assignment.getStatus());

        authenticate("student", "ROLE_STUDENT");
        submit();
        assertEquals(2, versions.size());
        assertEquals(2, versions.get(0).getRevision());

        authenticate("reviewer", "ROLE_REVIEWER");
        assertThrows(AppException.class,
                () -> service.review(3L, new DefenseReportService.Decision(1L, true, false, null)));
        service.review(3L, new DefenseReportService.Decision(2L, true, false, null));
        assertEquals(2L, assignment.getApprovedReportId());
        assertEquals("REVISION_REQUIRED", versions.get(1).getStatus());
    }

    @Test
    void terminalReviewerRejectionPublishesZeroImmediatelyAndOnlyOnce() {
        submit();
        authenticate("reviewer", "ROLE_REVIEWER");
        service.review(3L, new DefenseReportService.Decision(1L, false, true, "Không đủ điều kiện"));

        assertEquals(ReviewAssignmentStatusConstain.NOT_ELIGIBLE, assignment.getStatus());
        assertNotNull(assignment.getReportCheckedAt());
        verify(scores, times(1)).save(argThat(score -> score.getScore().signum() == 0
                && score.getStatus() == ScoreStatusConstain.LOCKED
                && Objects.equals(score.getAutomaticReviewAssignmentId(), 3L)
                && score.getLecture() == null));

        service.settleDeadline(3L);
        verify(scores, times(1)).save(any());
    }

    @Test
    void deadlineWithoutApprovalPublishesZero() {
        assignment.setDeadline(LocalDateTime.now(DefenseReportService.ZONE).minusMinutes(1));
        service.settleDeadline(3L);
        assertEquals(ReviewAssignmentStatusConstain.NOT_ELIGIBLE, assignment.getStatus());
        assertTrue(assignment.getReportFailureReason().contains("Chưa nộp hồ sơ"));
        verify(scores).save(any());
    }

    @Test
    void withdrawnStudentDoesNotReceiveAutomaticZero() {
        var enrollment = new GraduationEnrollmentEntity();
        enrollment.setStatus(EnrollmentStatusConstain.WITHDRAWN);
        when(enrollments.findByStudent_IdStudentAndDefensePeriod_ID_Defense("student-1", 1L))
                .thenReturn(Optional.of(enrollment));
        assignment.setDeadline(LocalDateTime.now(DefenseReportService.ZONE).minusMinutes(1));

        service.settleDeadline(3L);
        verify(scores, never()).save(any());
    }

    @Test
    void changingReviewerAfterApprovalKeepsTopicEligibleForScheduling() {
        submit();
        approveLatest();
        reviewerMember.setStatus(CommitteeMemberStatusConstain.INACTIVE);

        assertTrue(service.hasApprovedReview(2L, 4L));
        moveDefenseToPast();
        service.requireEligible(schedule);
    }

    @Test
    void wrongReviewerCannotReadOrDecide() {
        submit();
        authenticate("other", "ROLE_REVIEWER");
        assertThrows(AppException.class,
                () -> service.review(3L, new DefenseReportService.Decision(1L, true, false, null)));
        assertThrows(AppException.class, () -> service.get(3L));
    }

    @Test
    void uploadRequiresRealPdfOrDocxAndSafeLink() {
        assertThrows(AppException.class, () -> service.submit(3L, pdf(), null, null, null));
        assertThrows(AppException.class,
                () -> service.submit(3L, pdf(), "javascript:alert(1)", null, null));
        assertThrows(AppException.class,
                () -> service.submit(3L, new MockMultipartFile("file", "fake.pdf", "application/pdf", new byte[] {1}),
                        "https://example.com", null, null));
    }

    private void authenticate(String name, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                name, "", List.of(new SimpleGrantedAuthority(role))));
    }

    private MockMultipartFile pdf() {
        return new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "%PDF-1.7 test".getBytes());
    }

    private void submit() {
        service.submit(3L, pdf(), "https://github.com/example/project", null, "Ghi chú");
    }

    private void approveLatest() {
        authenticate("reviewer", "ROLE_REVIEWER");
        service.review(3L, new DefenseReportService.Decision(versions.get(0).getId(), true, false, null));
    }

    private void moveDefenseToPast() {
        schedule.setDefenseDate(LocalDate.now(DefenseReportService.ZONE).minusDays(1));
    }
}

package com.graduration.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.Constain.ReviewAssignmentStatusConstain;
import com.graduration.Constain.ReviewRecommendationConstain;
import com.graduration.Constain.StatusConstain;
import com.graduration.Constain.SupervisorAssignmentStatusConstain;
import com.graduration.Constain.TopicStatusConstain;
import com.graduration.DTO.Request.AssignReviewRequest;
import com.graduration.DTO.Request.CancelReviewRequest;
import com.graduration.DTO.Request.SubmitReviewRequest;
import com.graduration.Repository.LectureRepository;
import com.graduration.Repository.ReviewAssignmentRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.Service.ManagerService.ReviewAssignmentService;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.LectureEntity;
import com.graduration.entity.ReviewAssignmentEntity;
import com.graduration.entity.TeamEntity;
import com.graduration.entity.TopicEntity;
import com.graduration.entity.TopicSuperVisorEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.ReviewAssignmentMapper;

@ExtendWith(MockitoExtension.class)
class ReviewAssignmentServiceTest {
    @Mock
    ReviewAssignmentRepository reviewRepository;

    @Mock
    TopicRepository topicRepository;

    @Mock
    LectureRepository lectureRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    ReviewAssignmentMapper reviewMapper;

    @InjectMocks
    ReviewAssignmentService reviewService;

    @BeforeEach
    void setUp() {
        authenticate("admin", "ROLE_ADMIN");
        ReflectionTestUtils.setField(reviewService, "maxReviewersPerTopic", 1);
        ReflectionTestUtils.setField(reviewService, "maxAssignmentsPerPeriod", 5);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assign_createsAssignmentWithAuditData() {
        Fixture fixture = fixture();
        when(topicRepository.findById(9L)).thenReturn(Optional.of(fixture.topic));
        when(lectureRepository.findById("reviewer-1")).thenReturn(Optional.of(fixture.reviewer));
        when(userRepository.findById("admin")).thenReturn(Optional.of(fixture.admin));
        when(reviewRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        reviewService.assign(9L, assignRequest());

        ArgumentCaptor<ReviewAssignmentEntity> captor = ArgumentCaptor.forClass(ReviewAssignmentEntity.class);
        verify(reviewRepository).save(captor.capture());
        ReviewAssignmentEntity saved = captor.getValue();
        assertEquals(ReviewAssignmentStatusConstain.ASSIGNED, saved.getStatus());
        assertEquals("reviewer-1", saved.getLecture().getLectureId());
        assertEquals("admin", saved.getAssignedBy().getUserId());
        assertNotNull(saved.getAssignedAt());
    }

    @Test
    void assign_rejectsActiveSupervisorOfSameTopic() {
        Fixture fixture = fixture();
        fixture.topic
                .getTopicSuperVisorEntities()
                .add(TopicSuperVisorEntity.builder()
                        .lecture(fixture.reviewer)
                        .status(SupervisorAssignmentStatusConstain.ACTIVE)
                        .build());
        when(topicRepository.findById(9L)).thenReturn(Optional.of(fixture.topic));
        when(lectureRepository.findById("reviewer-1")).thenReturn(Optional.of(fixture.reviewer));

        AppException exception = assertThrows(AppException.class, () -> reviewService.assign(9L, assignRequest()));

        assertEquals(ErrorCode.REVIEW_SUPERVISOR_CONFLICT, exception.getErrorCode());
    }

    @Test
    void assign_rejectsDeadlineOutsideDefensePeriod() {
        Fixture fixture = fixture();
        when(topicRepository.findById(9L)).thenReturn(Optional.of(fixture.topic));
        when(lectureRepository.findById("reviewer-1")).thenReturn(Optional.of(fixture.reviewer));
        AssignReviewRequest request = assignRequest();
        request.setDeadline(LocalDateTime.now().plusMonths(3));

        AppException exception = assertThrows(AppException.class, () -> reviewService.assign(9L, request));

        assertEquals(ErrorCode.REVIEW_DEADLINE_INVALID, exception.getErrorCode());
    }

    @Test
    void submit_byAssignedReviewerStoresResult() {
        Fixture fixture = fixture();
        authenticate("reviewer-user", "ROLE_REVIEWER");
        ReviewAssignmentEntity assignment = assignment(fixture, ReviewAssignmentStatusConstain.IN_PROGRESS);
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(assignment));
        when(reviewRepository.save(assignment)).thenReturn(assignment);

        reviewService.submit(
                12L,
                SubmitReviewRequest.builder()
                        .reviewComment("Đề tài đạt yêu cầu")
                        .recommendation(ReviewRecommendationConstain.ELIGIBLE_FOR_DEFENSE)
                        .build());

        assertEquals(ReviewAssignmentStatusConstain.SUBMITTED, assignment.getStatus());
        assertEquals(ReviewRecommendationConstain.ELIGIBLE_FOR_DEFENSE, assignment.getRecommendation());
        assertNotNull(assignment.getSubmittedAt());
    }

    @Test
    void approve_requiresEligibleRecommendation() {
        Fixture fixture = fixture();
        ReviewAssignmentEntity assignment = assignment(fixture, ReviewAssignmentStatusConstain.SUBMITTED);
        assignment.setRecommendation(ReviewRecommendationConstain.REVISION_REQUIRED);
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(assignment));

        AppException exception = assertThrows(AppException.class, () -> reviewService.approve(12L));

        assertEquals(ErrorCode.REVIEW_OPERATION_NOT_ALLOWED, exception.getErrorCode());
    }

    @Test
    void cancel_keepsAssignmentAsHistory() {
        Fixture fixture = fixture();
        ReviewAssignmentEntity assignment = assignment(fixture, ReviewAssignmentStatusConstain.ASSIGNED);
        when(reviewRepository.findById(12L)).thenReturn(Optional.of(assignment));
        when(reviewRepository.save(assignment)).thenReturn(assignment);

        reviewService.cancel(
                12L, CancelReviewRequest.builder().reason("Phân công nhầm").build());

        assertEquals(ReviewAssignmentStatusConstain.CANCELLED, assignment.getStatus());
        assertEquals("Phân công nhầm", assignment.getCancelledReason());
        assertNotNull(assignment.getCancelledAt());
        verify(reviewRepository).save(assignment);
    }

    private AssignReviewRequest assignRequest() {
        return AssignReviewRequest.builder()
                .lectureId("reviewer-1")
                .deadline(LocalDateTime.now().plusDays(5))
                .note("Phản biện vòng 1")
                .build();
    }

    private ReviewAssignmentEntity assignment(Fixture fixture, ReviewAssignmentStatusConstain status) {
        return ReviewAssignmentEntity.builder()
                .reviewAssignmentId(12L)
                .topic(fixture.topic)
                .lecture(fixture.reviewer)
                .deadline(LocalDateTime.now().plusDays(5))
                .status(status)
                .build();
    }

    private Fixture fixture() {
        UserEntity admin = UserEntity.builder().userId("admin").build();
        UserEntity reviewerUser = UserEntity.builder()
                .userId("reviewer-user")
                .status(StatusConstain.ACTIVE)
                .build();
        LectureEntity reviewer = LectureEntity.builder()
                .lectureId("reviewer-1")
                .user(reviewerUser)
                .build();
        DefensePeriodEntity period = DefensePeriodEntity.builder()
                .ID_Defense(2L)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusMonths(2))
                .status(DefensePeriodConstain.ONGOING)
                .build();
        TopicEntity topic = TopicEntity.builder()
                .idTopic(9L)
                .status(TopicStatusConstain.REGISTERED)
                .defensePeriod(period)
                .team(TeamEntity.builder().idTeam(3L).build())
                .build();
        return new Fixture(admin, reviewer, topic);
    }

    private void authenticate(String userId, String authority) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        userId, null, List.of(new SimpleGrantedAuthority(authority))));
    }

    private record Fixture(UserEntity admin, LectureEntity reviewer, TopicEntity topic) {}
}

package com.graduration.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.graduration.Constain.StatusConstain;
import com.graduration.Constain.SupervisorAssignmentStatusConstain;
import com.graduration.Constain.SupervisorRoleConstain;
import com.graduration.Constain.TopicStatusConstain;
import com.graduration.DTO.Request.AssignTopicSupervisorRequest;
import com.graduration.DTO.Request.DeactivateTopicSupervisorRequest;
import com.graduration.Repository.LectureRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.Repository.TopicSupervisorRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.Service.ManagerService.TopicSupervisorService;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.LectureEntity;
import com.graduration.entity.TopicEntity;
import com.graduration.entity.TopicSuperVisorEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.TopicSupervisorMapper;

@ExtendWith(MockitoExtension.class)
class TopicSupervisorServiceTest {
    @Mock
    TopicSupervisorRepository supervisorRepository;

    @Mock
    TopicRepository topicRepository;

    @Mock
    LectureRepository lectureRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    TopicSupervisorMapper supervisorMapper;

    @InjectMocks
    TopicSupervisorService supervisorService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        ReflectionTestUtils.setField(supervisorService, "maxTopicsPerPeriod", 5);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assign_createsActivePrimaryAssignmentWithAuditUser() {
        Fixture fixture = fixture();
        when(topicRepository.findById(9L)).thenReturn(Optional.of(fixture.topic));
        when(lectureRepository.findById("lecture-1")).thenReturn(Optional.of(fixture.lecture));
        when(userRepository.findById("admin")).thenReturn(Optional.of(fixture.admin));
        when(supervisorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        supervisorService.assign(9L, request(SupervisorRoleConstain.PRIMARY));

        ArgumentCaptor<TopicSuperVisorEntity> captor = ArgumentCaptor.forClass(TopicSuperVisorEntity.class);
        verify(supervisorRepository).save(captor.capture());
        assertEquals(
                SupervisorAssignmentStatusConstain.ACTIVE, captor.getValue().getStatus());
        assertEquals(SupervisorRoleConstain.PRIMARY, captor.getValue().getSupervisorRole());
        assertEquals("admin", captor.getValue().getAssignedBy().getUserId());
    }

    @Test
    void assign_rejectsSecondActivePrimarySupervisor() {
        Fixture fixture = fixture();
        when(topicRepository.findById(9L)).thenReturn(Optional.of(fixture.topic));
        when(lectureRepository.findById("lecture-1")).thenReturn(Optional.of(fixture.lecture));
        when(supervisorRepository.existsByTopic_IdTopicAndSupervisorRoleAndStatus(
                        9L, SupervisorRoleConstain.PRIMARY, SupervisorAssignmentStatusConstain.ACTIVE))
                .thenReturn(true);

        AppException exception = assertThrows(
                AppException.class, () -> supervisorService.assign(9L, request(SupervisorRoleConstain.PRIMARY)));

        assertEquals(ErrorCode.TOPIC_PRIMARY_SUPERVISOR_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    void assign_rejectsInactiveLecturerAccount() {
        Fixture fixture = fixture();
        fixture.lecture.getUser().setStatus(StatusConstain.INACTIVE);
        when(topicRepository.findById(9L)).thenReturn(Optional.of(fixture.topic));
        when(lectureRepository.findById("lecture-1")).thenReturn(Optional.of(fixture.lecture));

        AppException exception = assertThrows(
                AppException.class, () -> supervisorService.assign(9L, request(SupervisorRoleConstain.CO_SUPERVISOR)));

        assertEquals(ErrorCode.LECTURER_INACTIVE, exception.getErrorCode());
    }

    @Test
    void assign_rejectsLecturerAtPeriodLimit() {
        Fixture fixture = fixture();
        when(topicRepository.findById(9L)).thenReturn(Optional.of(fixture.topic));
        when(lectureRepository.findById("lecture-1")).thenReturn(Optional.of(fixture.lecture));
        when(supervisorRepository.countActiveAssignments("lecture-1", 2L, SupervisorAssignmentStatusConstain.ACTIVE))
                .thenReturn(5L);

        AppException exception = assertThrows(
                AppException.class, () -> supervisorService.assign(9L, request(SupervisorRoleConstain.CO_SUPERVISOR)));

        assertEquals(ErrorCode.TOPIC_SUPERVISOR_LIMIT_REACHED, exception.getErrorCode());
    }

    @Test
    void deactivate_preservesAssignmentHistory() {
        Fixture fixture = fixture();
        TopicSuperVisorEntity assignment = TopicSuperVisorEntity.builder()
                .idSuperVisor(10L)
                .topic(fixture.topic)
                .lecture(fixture.lecture)
                .status(SupervisorAssignmentStatusConstain.ACTIVE)
                .note("Original")
                .build();
        when(supervisorRepository.findById(10L)).thenReturn(Optional.of(assignment));
        when(supervisorRepository.save(assignment)).thenReturn(assignment);

        supervisorService.deactivate(
                10L,
                DeactivateTopicSupervisorRequest.builder()
                        .reason("Changed lecturer")
                        .build());

        assertEquals(SupervisorAssignmentStatusConstain.INACTIVE, assignment.getStatus());
        assertEquals("Original | Deactivated: Changed lecturer", assignment.getNote());
        verify(supervisorRepository).save(assignment);
    }

    private AssignTopicSupervisorRequest request(SupervisorRoleConstain role) {
        return AssignTopicSupervisorRequest.builder()
                .lectureId("lecture-1")
                .role(role)
                .note("Assignment")
                .build();
    }

    private Fixture fixture() {
        UserEntity admin = UserEntity.builder().userId("admin").build();
        UserEntity lecturerUser = UserEntity.builder()
                .userId("lecturer-user")
                .status(StatusConstain.ACTIVE)
                .build();
        LectureEntity lecture = LectureEntity.builder()
                .lectureId("lecture-1")
                .user(lecturerUser)
                .build();
        DefensePeriodEntity period = DefensePeriodEntity.builder()
                .ID_Defense(2L)
                .status(DefensePeriodConstain.ONGOING)
                .build();
        TopicEntity topic = TopicEntity.builder()
                .idTopic(9L)
                .status(TopicStatusConstain.APPROVED)
                .defensePeriod(period)
                .build();
        return new Fixture(admin, lecture, topic);
    }

    private record Fixture(UserEntity admin, LectureEntity lecture, TopicEntity topic) {}
}

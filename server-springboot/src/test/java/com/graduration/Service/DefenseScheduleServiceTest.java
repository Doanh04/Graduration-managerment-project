package com.graduration.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
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

import com.graduration.Constain.CommitteeMemberRoleConstain;
import com.graduration.Constain.CommitteeMemberStatusConstain;
import com.graduration.Constain.DefenseCommitteeStatusConstain;
import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.Constain.DefenseScheduleConflictTypeConstain;
import com.graduration.Constain.DefenseScheduleStatusConstain;
import com.graduration.Constain.SupervisorAssignmentStatusConstain;
import com.graduration.Constain.TopicStatusConstain;
import com.graduration.DTO.Request.DefenseScheduleRequest;
import com.graduration.DTO.Request.ScheduleReasonRequest;
import com.graduration.DTO.Response.DefenseScheduleValidationResponse;
import com.graduration.Repository.DefenseCommitteeRepository;
import com.graduration.Repository.DefensePeriodRepository;
import com.graduration.Repository.DefenseScheduleRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.Service.ManagerService.DefenseScheduleHistoryService;
import com.graduration.Service.ManagerService.DefenseScheduleService;
import com.graduration.entity.ComitteesMemberEntity;
import com.graduration.entity.DefenseCommitteesEntity;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.DefenseSchedulesEntity;
import com.graduration.entity.LectureEntity;
import com.graduration.entity.TeamEntity;
import com.graduration.entity.TopicEntity;
import com.graduration.entity.TopicSuperVisorEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.DefenseScheduleMapper;

@ExtendWith(MockitoExtension.class)
class DefenseScheduleServiceTest {
    @Mock
    DefenseScheduleRepository scheduleRepository;

    @Mock
    DefensePeriodRepository periodRepository;

    @Mock
    DefenseCommitteeRepository committeeRepository;

    @Mock
    TopicRepository topicRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    DefenseScheduleMapper scheduleMapper;

    @Mock
    DefenseScheduleHistoryService historyService;

    @InjectMocks
    DefenseScheduleService scheduleService;

    @BeforeEach
    void authenticateAdmin() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_savesValidScheduleAsDraft() {
        Fixture fixture = fixture();
        stubEntities(fixture);
        when(userRepository.findById("admin"))
                .thenReturn(Optional.of(UserEntity.builder().userId("admin").build()));
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        scheduleService.create(2L, request());

        ArgumentCaptor<DefenseSchedulesEntity> captor = ArgumentCaptor.forClass(DefenseSchedulesEntity.class);
        verify(scheduleRepository).save(captor.capture());
        assertEquals(DefenseScheduleStatusConstain.DRAFT, captor.getValue().getStatus());
        assertEquals("A101", captor.getValue().getRoom());
        assertEquals(fixture.topic, captor.getValue().getTopic());
    }

    @Test
    void validate_reportsRoomConflict() {
        Fixture fixture = fixture();
        stubEntities(fixture);
        when(scheduleRepository.hasRoomConflict(any(), any(), any(), any(), any(), any()))
                .thenReturn(true);

        DefenseScheduleValidationResponse result = scheduleService.validate(2L, request());

        assertFalse(result.isValid());
        assertEquals(
                DefenseScheduleConflictTypeConstain.ROOM_CONFLICT,
                result.getConflicts().get(0).getType());
    }

    @Test
    void validate_reportsSupervisorReviewerConflict() {
        Fixture fixture = fixture();
        LectureEntity lecturer = LectureEntity.builder().lectureId("lecture-1").build();
        fixture.topic
                .getTopicSuperVisorEntities()
                .add(TopicSuperVisorEntity.builder()
                        .lecture(lecturer)
                        .status(SupervisorAssignmentStatusConstain.ACTIVE)
                        .build());
        fixture.committee
                .getComitteesMember()
                .add(ComitteesMemberEntity.builder()
                        .lecture(lecturer)
                        .role(CommitteeMemberRoleConstain.REVIEWER)
                        .status(CommitteeMemberStatusConstain.ACTIVE)
                        .build());
        stubEntities(fixture);

        DefenseScheduleValidationResponse result = scheduleService.validate(2L, request());

        assertFalse(result.isValid());
        assertEquals(
                DefenseScheduleConflictTypeConstain.SUPERVISOR_REVIEWER_CONFLICT,
                result.getConflicts().get(0).getType());
    }

    @Test
    void publish_revalidatesAndChangesDraftToPublished() {
        Fixture fixture = fixture();
        DefenseSchedulesEntity schedule = schedule(fixture, DefenseScheduleStatusConstain.DRAFT);
        when(scheduleRepository.findById(20L)).thenReturn(Optional.of(schedule));
        when(periodRepository.findById(2L)).thenReturn(Optional.of(fixture.period));
        when(scheduleRepository.save(schedule)).thenReturn(schedule);
        when(userRepository.findById("admin"))
                .thenReturn(Optional.of(UserEntity.builder().userId("admin").build()));

        scheduleService.publish(20L);

        assertEquals(DefenseScheduleStatusConstain.PUBLISHED, schedule.getStatus());
        verify(scheduleRepository).save(schedule);
    }

    @Test
    void postpone_keepsReasonAndChangesPublishedSchedule() {
        Fixture fixture = fixture();
        DefenseSchedulesEntity schedule = schedule(fixture, DefenseScheduleStatusConstain.PUBLISHED);
        when(scheduleRepository.findById(20L)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(schedule)).thenReturn(schedule);
        when(userRepository.findById("admin"))
                .thenReturn(Optional.of(UserEntity.builder().userId("admin").build()));

        scheduleService.postpone(
                20L,
                ScheduleReasonRequest.builder().reason(" Lecturer unavailable ").build());

        assertEquals(DefenseScheduleStatusConstain.POSTPONED, schedule.getStatus());
        assertEquals("Lecturer unavailable", schedule.getPostponedReason());
    }

    @Test
    void delete_rejectsPublishedSchedule() {
        Fixture fixture = fixture();
        DefenseSchedulesEntity schedule = schedule(fixture, DefenseScheduleStatusConstain.PUBLISHED);
        when(scheduleRepository.findById(20L)).thenReturn(Optional.of(schedule));

        AppException exception = assertThrows(AppException.class, () -> scheduleService.delete(20L));

        assertEquals(ErrorCode.DEFENSE_SCHEDULE_OPERATION_NOT_ALLOWED, exception.getErrorCode());
    }

    private void stubEntities(Fixture fixture) {
        when(periodRepository.findById(2L)).thenReturn(Optional.of(fixture.period));
        when(topicRepository.findById(9L)).thenReturn(Optional.of(fixture.topic));
        when(committeeRepository.findById(3L)).thenReturn(Optional.of(fixture.committee));
    }

    private DefenseScheduleRequest request() {
        return DefenseScheduleRequest.builder()
                .topicId(9L)
                .committeeId(3L)
                .defenseDate(LocalDate.of(2026, 12, 10))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(8, 45))
                .room(" A101 ")
                .location("Building A")
                .build();
    }

    private DefenseSchedulesEntity schedule(Fixture fixture, DefenseScheduleStatusConstain status) {
        return DefenseSchedulesEntity.builder()
                .idDefenseScheduce(20L)
                .topic(fixture.topic)
                .defenseCommittees(fixture.committee)
                .defenseDate(LocalDate.of(2026, 12, 10))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(8, 45))
                .room("A101")
                .location("Building A")
                .status(status)
                .build();
    }

    private Fixture fixture() {
        DefensePeriodEntity period = DefensePeriodEntity.builder()
                .ID_Defense(2L)
                .startDate(LocalDate.of(2026, 12, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .status(DefensePeriodConstain.ONGOING)
                .build();
        TeamEntity team = TeamEntity.builder().idTeam(5L).build();
        TopicEntity topic = TopicEntity.builder()
                .idTopic(9L)
                .status(TopicStatusConstain.REGISTERED)
                .defensePeriod(period)
                .team(team)
                .build();
        DefenseCommitteesEntity committee = DefenseCommitteesEntity.builder()
                .idComittees(3L)
                .status(DefenseCommitteeStatusConstain.ACTIVE)
                .defensePeriod(period)
                .build();
        return new Fixture(period, topic, committee);
    }

    private record Fixture(DefensePeriodEntity period, TopicEntity topic, DefenseCommitteesEntity committee) {}
}

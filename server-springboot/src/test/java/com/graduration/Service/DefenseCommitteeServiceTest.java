package com.graduration.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import com.graduration.Constain.CommitteeMemberRoleConstain;
import com.graduration.Constain.CommitteeMemberStatusConstain;
import com.graduration.Constain.DefenseCommitteeStatusConstain;
import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.Constain.DefenseScheduleStatusConstain;
import com.graduration.Constain.StatusConstain;
import com.graduration.DTO.Request.DefenseCommitteeRequest;
import com.graduration.DTO.Response.DefenseCommitteeValidationResponse;
import com.graduration.Repository.DefenseCommitteeRepository;
import com.graduration.Repository.DefensePeriodRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.Service.ManagerService.DefenseCommitteeService;
import com.graduration.entity.AcademicYearEntity;
import com.graduration.entity.ComitteesMemberEntity;
import com.graduration.entity.DefenseCommitteesEntity;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.DefenseSchedulesEntity;
import com.graduration.entity.LectureEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.DefenseCommitteeMapper;

@ExtendWith(MockitoExtension.class)
class DefenseCommitteeServiceTest {
    @Mock
    DefenseCommitteeRepository committeeRepository;

    @Mock
    DefensePeriodRepository defensePeriodRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    DefenseCommitteeMapper committeeMapper;

    @InjectMocks
    DefenseCommitteeService committeeService;

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
    void create_derivesAcademicYearFromDefensePeriodAndStartsAsDraft() {
        Fixture fixture = fixture();
        when(defensePeriodRepository.findById(2L)).thenReturn(Optional.of(fixture.period));
        when(userRepository.findById("admin")).thenReturn(Optional.of(fixture.admin));
        when(committeeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        committeeService.create(2L, request(" Council 1 "));

        ArgumentCaptor<DefenseCommitteesEntity> captor = ArgumentCaptor.forClass(DefenseCommitteesEntity.class);
        verify(committeeRepository).save(captor.capture());
        assertEquals("Council 1", captor.getValue().getComitteesName());
        assertEquals(DefenseCommitteeStatusConstain.DRAFT, captor.getValue().getStatus());
        assertEquals(fixture.academicYear, captor.getValue().getAcademicYear());
    }

    @Test
    void validation_reportsMissingRequiredRoles() {
        Fixture fixture = fixture();
        fixture.committee.getComitteesMember().add(member(CommitteeMemberRoleConstain.CHAIRPERSON));
        when(committeeRepository.findById(3L)).thenReturn(Optional.of(fixture.committee));

        DefenseCommitteeValidationResponse result = committeeService.validate(3L);

        assertFalse(result.isValid());
        assertEquals(1, result.getChairpersonCount());
        assertEquals(0, result.getSecretaryCount());
        assertTrue(result.getErrors().size() >= 3);
    }

    @Test
    void activate_acceptsValidCommitteeStructure() {
        Fixture fixture = fixture();
        fixture.committee
                .getComitteesMember()
                .addAll(List.of(
                        member(CommitteeMemberRoleConstain.CHAIRPERSON),
                        member(CommitteeMemberRoleConstain.SECRETARY),
                        member(CommitteeMemberRoleConstain.REVIEWER)));
        when(committeeRepository.findById(3L)).thenReturn(Optional.of(fixture.committee));
        when(defensePeriodRepository.findById(2L)).thenReturn(Optional.of(fixture.period));
        when(committeeRepository.save(fixture.committee)).thenReturn(fixture.committee);

        committeeService.activate(3L);

        assertEquals(DefenseCommitteeStatusConstain.ACTIVE, fixture.committee.getStatus());
        verify(committeeRepository).save(fixture.committee);
    }

    @Test
    void activate_rejectsInvalidCommitteeStructure() {
        Fixture fixture = fixture();
        when(committeeRepository.findById(3L)).thenReturn(Optional.of(fixture.committee));
        when(defensePeriodRepository.findById(2L)).thenReturn(Optional.of(fixture.period));

        AppException exception = assertThrows(AppException.class, () -> committeeService.activate(3L));

        assertEquals(ErrorCode.DEFENSE_COMMITTEE_NOT_READY, exception.getErrorCode());
    }

    @Test
    void moveToDraft_rejectsCommitteeWithNonCancelledSchedule() {
        Fixture fixture = fixture();
        fixture.committee.setStatus(DefenseCommitteeStatusConstain.ACTIVE);
        fixture.committee
                .getDefenseSchedules()
                .add(DefenseSchedulesEntity.builder()
                        .status(DefenseScheduleStatusConstain.SCHEDULED)
                        .build());
        when(committeeRepository.findById(3L)).thenReturn(Optional.of(fixture.committee));

        AppException exception = assertThrows(AppException.class, () -> committeeService.moveToDraft(3L));

        assertEquals(ErrorCode.DEFENSE_COMMITTEE_IN_USE, exception.getErrorCode());
    }

    private ComitteesMemberEntity member(CommitteeMemberRoleConstain role) {
        UserEntity user = UserEntity.builder()
                .userId("user-" + role)
                .status(StatusConstain.ACTIVE)
                .build();
        LectureEntity lecture =
                LectureEntity.builder().lectureId("lecture-" + role).user(user).build();
        return ComitteesMemberEntity.builder()
                .lecture(lecture)
                .role(role)
                .status(CommitteeMemberStatusConstain.ACTIVE)
                .build();
    }

    private DefenseCommitteeRequest request(String name) {
        return DefenseCommitteeRequest.builder()
                .committeeName(name)
                .description("Description")
                .build();
    }

    private Fixture fixture() {
        UserEntity admin = UserEntity.builder().userId("admin").build();
        AcademicYearEntity academicYear = AcademicYearEntity.builder()
                .academicId(1)
                .academicYear("2026-2027")
                .build();
        DefensePeriodEntity period = DefensePeriodEntity.builder()
                .ID_Defense(2L)
                .status(DefensePeriodConstain.ONGOING)
                .academicYear(academicYear)
                .build();
        DefenseCommitteesEntity committee = DefenseCommitteesEntity.builder()
                .idComittees(3L)
                .comitteesName("Council 1")
                .status(DefenseCommitteeStatusConstain.DRAFT)
                .defensePeriod(period)
                .academicYear(academicYear)
                .build();
        return new Fixture(admin, academicYear, period, committee);
    }

    private record Fixture(
            UserEntity admin,
            AcademicYearEntity academicYear,
            DefensePeriodEntity period,
            DefenseCommitteesEntity committee) {}
}

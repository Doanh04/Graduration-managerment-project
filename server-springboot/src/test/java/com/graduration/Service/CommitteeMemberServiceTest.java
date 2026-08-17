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

import com.graduration.Constain.CommitteeMemberRoleConstain;
import com.graduration.Constain.CommitteeMemberStatusConstain;
import com.graduration.Constain.DefenseCommitteeStatusConstain;
import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.Constain.StatusConstain;
import com.graduration.DTO.Request.AssignCommitteeMemberRequest;
import com.graduration.DTO.Request.DeactivateCommitteeMemberRequest;
import com.graduration.Repository.CommitteeMemberRepository;
import com.graduration.Repository.DefenseCommitteeRepository;
import com.graduration.Repository.LectureRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.Service.ManagerService.CommitteeMemberService;
import com.graduration.entity.ComitteesMemberEntity;
import com.graduration.entity.DefenseCommitteesEntity;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.LectureEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.CommitteeMemberMapper;

@ExtendWith(MockitoExtension.class)
class CommitteeMemberServiceTest {
    @Mock
    CommitteeMemberRepository memberRepository;

    @Mock
    DefenseCommitteeRepository committeeRepository;

    @Mock
    LectureRepository lectureRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    CommitteeMemberMapper memberMapper;

    @InjectMocks
    CommitteeMemberService memberService;

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
    void assign_createsActiveMemberAndAuditInformation() {
        Fixture fixture = fixture();
        when(committeeRepository.findById(3L)).thenReturn(Optional.of(fixture.committee));
        when(lectureRepository.findById("lecture-1")).thenReturn(Optional.of(fixture.lecture));
        when(userRepository.findById("admin")).thenReturn(Optional.of(fixture.admin));
        when(memberRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        memberService.assign(3L, request(CommitteeMemberRoleConstain.CHAIRPERSON));

        ArgumentCaptor<ComitteesMemberEntity> captor = ArgumentCaptor.forClass(ComitteesMemberEntity.class);
        verify(memberRepository).save(captor.capture());
        assertEquals(CommitteeMemberStatusConstain.ACTIVE, captor.getValue().getStatus());
        assertEquals(CommitteeMemberRoleConstain.CHAIRPERSON, captor.getValue().getRole());
        assertEquals("admin", captor.getValue().getAssignedBy().getUserId());
    }

    @Test
    void assign_rejectsDuplicateActiveLecturer() {
        Fixture fixture = fixture();
        when(committeeRepository.findById(3L)).thenReturn(Optional.of(fixture.committee));
        when(lectureRepository.findById("lecture-1")).thenReturn(Optional.of(fixture.lecture));
        when(memberRepository.existsByDefenseCommittees_IdComitteesAndLecture_LectureIdAndStatus(
                        3L, "lecture-1", CommitteeMemberStatusConstain.ACTIVE))
                .thenReturn(true);

        AppException exception = assertThrows(
                AppException.class, () -> memberService.assign(3L, request(CommitteeMemberRoleConstain.MEMBER)));

        assertEquals(ErrorCode.COMMITTEE_MEMBER_ALREADY_ASSIGNED, exception.getErrorCode());
    }

    @Test
    void assign_rejectsSecondChairperson() {
        Fixture fixture = fixture();
        when(committeeRepository.findById(3L)).thenReturn(Optional.of(fixture.committee));
        when(lectureRepository.findById("lecture-1")).thenReturn(Optional.of(fixture.lecture));
        when(memberRepository.existsByDefenseCommittees_IdComitteesAndRoleAndStatus(
                        3L, CommitteeMemberRoleConstain.CHAIRPERSON, CommitteeMemberStatusConstain.ACTIVE))
                .thenReturn(true);

        AppException exception = assertThrows(
                AppException.class, () -> memberService.assign(3L, request(CommitteeMemberRoleConstain.CHAIRPERSON)));

        assertEquals(ErrorCode.COMMITTEE_CHAIRPERSON_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    void assign_rejectsFinishedDefensePeriod() {
        Fixture fixture = fixture();
        fixture.committee.getDefensePeriod().setStatus(DefensePeriodConstain.FINISHED);
        when(committeeRepository.findById(3L)).thenReturn(Optional.of(fixture.committee));

        AppException exception = assertThrows(
                AppException.class, () -> memberService.assign(3L, request(CommitteeMemberRoleConstain.MEMBER)));

        assertEquals(ErrorCode.DEFENSE_PERIOD_FINISHED, exception.getErrorCode());
    }

    @Test
    void deactivate_keepsHistoryInsteadOfDeletingMember() {
        Fixture fixture = fixture();
        ComitteesMemberEntity member = ComitteesMemberEntity.builder()
                .comitteesMemberId(8L)
                .defenseCommittees(fixture.committee)
                .lecture(fixture.lecture)
                .role(CommitteeMemberRoleConstain.SECRETARY)
                .status(CommitteeMemberStatusConstain.ACTIVE)
                .note("Original")
                .build();
        when(memberRepository.findById(8L)).thenReturn(Optional.of(member));
        when(committeeRepository.findById(3L)).thenReturn(Optional.of(fixture.committee));
        when(memberRepository.save(member)).thenReturn(member);

        memberService.deactivate(
                8L,
                DeactivateCommitteeMemberRequest.builder().reason("Unavailable").build());

        assertEquals(CommitteeMemberStatusConstain.INACTIVE, member.getStatus());
        assertEquals("Original | Deactivated: Unavailable", member.getNote());
        verify(memberRepository).save(member);
    }

    private AssignCommitteeMemberRequest request(CommitteeMemberRoleConstain role) {
        return AssignCommitteeMemberRequest.builder()
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
        DefenseCommitteesEntity committee = DefenseCommitteesEntity.builder()
                .idComittees(3L)
                .comitteesName("Council 1")
                .status(DefenseCommitteeStatusConstain.DRAFT)
                .defensePeriod(period)
                .build();
        return new Fixture(admin, lecture, committee);
    }

    private record Fixture(UserEntity admin, LectureEntity lecture, DefenseCommitteesEntity committee) {}
}

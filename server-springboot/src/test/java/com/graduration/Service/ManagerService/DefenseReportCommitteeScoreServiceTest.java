package com.graduration.Service.ManagerService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.graduration.Constain.*;
import com.graduration.DTO.Request.CommitteeScoreEntryRequest;
import com.graduration.DTO.Request.CommitteeScoreSheetRequest;
import com.graduration.Repository.*;
import com.graduration.Service.GradurationService.GraduationEnrollmentService;
import com.graduration.Service.GradurationService.ScoreService;
import com.graduration.entity.*;
import com.graduration.mapper.ScoreMapper;

@ExtendWith(MockitoExtension.class)
class DefenseReportCommitteeScoreServiceTest {
    @Mock ScoreRepository scores;
    @Mock LectureRepository lectures;
    @Mock CommitteeMemberRepository committeeMembers;
    @Mock StudentRepository students;
    @Mock TopicRepository topics;
    @Mock GraduationEnrollmentRepository enrollments;
    @Mock ScoreMapper mapper;
    @Mock GraduationEnrollmentService enrollmentService;
    @Mock DefenseReportService defenseReportService;
    @InjectMocks ScoreService service;

    private StudentEntity student;
    private TopicEntity topic;
    private List<ComitteesMemberEntity> members;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        student = StudentEntity.builder()
                .idStudent("student-1")
                .studentCode("SV001")
                .fullNameStudent("Sinh viên 1")
                .build();
        TeamEntity team = TeamEntity.builder().idTeam(7L).studentEntities(List.of(student)).build();
        DefensePeriodEntity period = DefensePeriodEntity.builder().ID_Defense(2L).build();
        DefenseCommitteesEntity committee = DefenseCommitteesEntity.builder()
                .idComittees(20L)
                .comitteesName("Hội đồng 1")
                .build();
        topic = TopicEntity.builder()
                .idTopic(9L)
                .title("Đề tài")
                .team(team)
                .defensePeriod(period)
                .build();
        topic.setDefenseSchedule(DefenseSchedulesEntity.builder()
                .defenseCommittees(committee)
                .topic(topic)
                .build());
        members = List.of(
                member(committee, "l1", CommitteeMemberRoleConstain.CHAIRPERSON),
                member(committee, "l2", CommitteeMemberRoleConstain.SECRETARY),
                member(committee, "l3", CommitteeMemberRoleConstain.REVIEWER));
        when(students.findById("student-1")).thenReturn(Optional.of(student));
        when(topics.findById(9L)).thenReturn(Optional.of(topic));
        when(committeeMembers.findByDefenseCommittees_IdComitteesAndStatusOrderByRoleAsc(
                        20L, CommitteeMemberStatusConstain.ACTIVE))
                .thenReturn(members);
        when(scores.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void secretaryEntersScoresButAverageUsesOnlyChairpersonAndReviewer() {
        when(scores.findCommitteeScores("student-1", 9L)).thenReturn(List.of());

        var response = service.saveCommitteeScoreSheet("student-1", 9L, CommitteeScoreSheetRequest.builder()
                .entries(List.of(entry("l1", "8.00"), entry("l3", "10.00")))
                .build());

        assertEquals(new BigDecimal("9.00"), response.getFinalScore());
        assertEquals(2, response.getScoredMembers());
        assertEquals(2, response.getTotalMembers());
        assertEquals(2, response.getEntries().size());
        assertTrue(response.getEntries().stream()
                .noneMatch(entry -> entry.getRole() == CommitteeMemberRoleConstain.SECRETARY));
        assertTrue(response.isComplete());
        assertFalse(response.isPublished());
    }

    @Test
    void submittingSheetLocksAllMemberScores() {
        List<ScoreEntity> rows = List.of(
                score(members.get(0), "8.00"),
                score(members.get(1), "2.00"),
                score(members.get(2), "10.00"));
        when(scores.findCommitteeScores("student-1", 9L)).thenReturn(rows);

        var response = service.submitCommitteeScoreSheet("student-1", 9L);

        assertFalse(response.isPublished());
        assertEquals(new BigDecimal("9.00"), response.getFinalScore());
        assertEquals(ScoreStatusConstain.LOCKED, rows.get(0).getStatus());
        assertEquals(ScoreStatusConstain.DRAFT, rows.get(1).getStatus());
        assertEquals(ScoreStatusConstain.LOCKED, rows.get(2).getStatus());
    }

    private ComitteesMemberEntity member(
            DefenseCommitteesEntity committee, String lecturerId, CommitteeMemberRoleConstain role) {
        return ComitteesMemberEntity.builder()
                .defenseCommittees(committee)
                .lecture(LectureEntity.builder()
                        .lectureId(lecturerId)
                        .lectureCode(lecturerId.toUpperCase())
                        .fullNameLecture("Giảng viên " + lecturerId)
                        .build())
                .role(role)
                .status(CommitteeMemberStatusConstain.ACTIVE)
                .build();
    }

    private CommitteeScoreEntryRequest entry(String lecturerId, String value) {
        return CommitteeScoreEntryRequest.builder()
                .lecturerId(lecturerId)
                .score(new BigDecimal(value))
                .comment("Nhận xét " + lecturerId)
                .build();
    }

    private ScoreEntity score(ComitteesMemberEntity member, String value) {
        return ScoreEntity.builder()
                .student(student)
                .topic(topic)
                .lecture(member.getLecture())
                .score(new BigDecimal(value))
                .comment("Nhận xét " + member.getLecture().getLectureId())
                .scoreType(ScoreTypeConstain.FINAL)
                .status(ScoreStatusConstain.DRAFT)
                .build();
    }
}

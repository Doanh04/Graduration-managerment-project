package com.graduration.Service.GradurationService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.graduration.Constain.*;
import com.graduration.DTO.Request.CreateSupervisorTopicProposalRequest;
import com.graduration.DTO.Response.TopicResponse;
import com.graduration.Repository.*;
import com.graduration.Service.SystemSettingService;
import com.graduration.Service.NotificationService.NotificationRecipientService;
import com.graduration.Service.NotificationService.NotificationService;
import com.graduration.entity.*;
import com.graduration.exception.*;
import com.graduration.mapper.TopicMapper;

class SupervisorProposalTeamNameTest {
    TopicRepository topics = mock(TopicRepository.class);
    TopicRegistrationRepository registrations = mock(TopicRegistrationRepository.class);
    DefensePeriodRepository periods = mock(DefensePeriodRepository.class);
    UserRepository users = mock(UserRepository.class);
    TeamRepository teams = mock(TeamRepository.class);
    TopicMapper mapper = mock(TopicMapper.class);
    StudentRepository students = mock(StudentRepository.class);
    LectureRepository lectures = mock(LectureRepository.class);
    TopicSupervisorRepository supervisors = mock(TopicSupervisorRepository.class);
    GraduationEnrollmentService enrollments = mock(GraduationEnrollmentService.class);
    SystemSettingService settings = mock(SystemSettingService.class);
    NotificationService notifications = mock(NotificationService.class);
    NotificationRecipientService notificationRecipients = mock(NotificationRecipientService.class);
    TopicService service;
    DefensePeriodEntity period;

    @BeforeEach void setup() {
        service = new TopicService(topics, registrations, periods, users, teams, mapper, students, lectures,
                supervisors, enrollments, settings, notifications, notificationRecipients);
        period = DefensePeriodEntity.builder().ID_Defense(10L).periodName("Đợt 1")
                .startDate(LocalDate.now().minusDays(1)).endDate(LocalDate.now().plusDays(1))
                .status(DefensePeriodConstain.ONGOING).build();
        when(periods.findById(10L)).thenReturn(Optional.of(period));
        when(teams.save(any())).thenAnswer(inv -> { TeamEntity team = inv.getArgument(0); team.setIdTeam(20L); return team; });
        when(topics.save(any())).thenAnswer(inv -> { TopicEntity topic = inv.getArgument(0); topic.setIdTopic(30L); return topic; });
        when(mapper.toResponse(any())).thenReturn(TopicResponse.builder().topicId(30L).build());
        when(enrollments.ensureEligible(any(), eq(period), anyString())).thenReturn(new GraduationEnrollmentEntity());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "lecturer-user", "", List.of(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void oneStudentGetsAutomaticTeamNameEvenWhenClientOmitsIt() {
        var student = student("SV001", "Nguyễn Văn An");
        when(students.findAllByStudentCodeIn(any())).thenReturn(List.of(student));
        service.createSupervisorProposal(request(null, List.of("SV001")));
        var capture = ArgumentCaptor.forClass(TeamEntity.class);
        verify(teams).save(capture.capture());
        assertEquals("Nhóm Nguyễn Văn An", capture.getValue().getNameTeam());
    }

    @Test void duplicateAutomaticNameAddsStudentCode() {
        var student = student("SV001", "Nguyễn Văn An");
        when(students.findAllByStudentCodeIn(any())).thenReturn(List.of(student));
        when(teams.existsByNameAndDefensePeriod("Nhóm Nguyễn Văn An", 10L)).thenReturn(true);
        service.createSupervisorProposal(request("Tên từ client bị bỏ qua", List.of("SV001")));
        var capture = ArgumentCaptor.forClass(TeamEntity.class);
        verify(teams).save(capture.capture());
        assertEquals("Nhóm Nguyễn Văn An - SV001", capture.getValue().getNameTeam());
    }

    @Test void twoStudentsRequireExplicitTeamName() {
        when(students.findAllByStudentCodeIn(any())).thenReturn(List.of(student("SV001", "An"), student("SV002", "Bình")));
        var error = assertThrows(AppException.class,
                () -> service.createSupervisorProposal(request(" ", List.of("SV001", "SV002"))));
        assertEquals(ErrorCode.TEAM_NAME_NOT_BLANK, error.getErrorCode());
        verify(teams, never()).save(any());
    }

    @Test void twoStudentsUseEnteredTeamName() {
        when(students.findAllByStudentCodeIn(any())).thenReturn(List.of(student("SV001", "An"), student("SV002", "Bình")));
        service.createSupervisorProposal(request("  Nhóm AI  ", List.of("SV001", "SV002")));
        var capture = ArgumentCaptor.forClass(TeamEntity.class);
        verify(teams).save(capture.capture());
        assertEquals("Nhóm AI", capture.getValue().getNameTeam());
    }

    @Test void proposalNotifiesAdminAndFacultyWithLecturerAndContent() {
        when(students.findAllByStudentCodeIn(any())).thenReturn(List.of(student("SV001", "Nguyễn Văn An")));
        when(lectures.findByUser_UserId("lecturer-user")).thenReturn(Optional.of(LectureEntity.builder()
                .fullNameLecture("Nguyễn Tuấn Anh")
                .lectureCode("LEC001")
                .build()));
        var request = request(null, List.of("SV001"));
        request.setDescription("Xây dựng hệ thống gợi ý");

        service.createSupervisorProposal(request);

        verify(notifications).notifyRoles(
                eq(Set.of(RoleConstain.ADMIN, RoleConstain.FACULTY)),
                eq("Giảng viên gửi đề xuất đề tài"),
                argThat(content -> content.contains("Nguyễn Tuấn Anh")
                        && content.contains("LEC001")
                        && content.contains("Đề tài AI")
                        && content.contains("Nhóm Nguyễn Văn An")
                        && content.contains("Xây dựng hệ thống gợi ý")),
                eq(NotificationTypeConstain.TOPIC_PROPOSED),
                eq("TOPIC"),
                eq(30L));
    }

    private StudentEntity student(String code, String name) {
        return StudentEntity.builder().idStudent("id-" + code).studentCode(code).fullNameStudent(name).build();
    }
    private CreateSupervisorTopicProposalRequest request(String teamName, List<String> codes) {
        return CreateSupervisorTopicProposalRequest.builder().title("Đề tài AI").defensePeriodId(10L)
                .teamName(teamName).studentCodes(codes).build();
    }
}

package com.graduration.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.Constain.MilesStoneStatusConstain;
import com.graduration.Constain.MilesStoneTypeConstain;
import com.graduration.Constain.SubmissionStatusConstain;
import com.graduration.Repository.CommentRepository;
import com.graduration.Repository.GraduationEnrollmentRepository;
import com.graduration.Repository.LectureRepository;
import com.graduration.Repository.MilestoneRepository;
import com.graduration.Repository.StudentRepository;
import com.graduration.Repository.SubmissionRepository;
import com.graduration.Repository.TeamRepository;
import com.graduration.Service.GradurationService.FileStorageService;
import com.graduration.Service.GradurationService.SubmissionService;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.MilesStoneEntity;
import com.graduration.entity.StudentEntity;
import com.graduration.entity.SubmistionEntity;
import com.graduration.entity.TeamEntity;
import com.graduration.entity.TopicEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.SubmissionMapper;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {
    @Mock
    SubmissionRepository submissionRepository;

    @Mock
    MilestoneRepository milestoneRepository;

    @Mock
    TeamRepository teamRepository;

    @Mock
    StudentRepository studentRepository;

    @Mock
    LectureRepository lectureRepository;

    @Mock
    GraduationEnrollmentRepository enrollmentRepository;

    @Mock
    CommentRepository commentRepository;

    @Mock
    FileStorageService fileStorageService;

    @Mock
    SubmissionMapper submissionMapper;

    @InjectMocks
    SubmissionService submissionService;

    @BeforeEach
    void authenticateStudent() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "user-1", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void upload_createsFirstVersionAndMetadata() {
        Fixture fixture = fixture();
        stubFixture(fixture);
        when(fileStorageService.store(any(), any(), any(), any(), any()))
                .thenReturn(new FileStorageService.StoredFile("relative/file.pdf", "uuid.pdf", "sha256"));
        when(submissionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        submissionService.upload(5L, 10L, " First report ", pdf(), LocalDateTime.of(2026, 9, 10, 8, 0));

        ArgumentCaptor<SubmistionEntity> captor = ArgumentCaptor.forClass(SubmistionEntity.class);
        verify(submissionRepository).save(captor.capture());
        SubmistionEntity submission = captor.getValue();
        assertEquals(1, submission.getVersion());
        assertEquals(SubmissionStatusConstain.SUBMITTED, submission.getStatus());
        assertEquals(false, submission.getIsLate());
        assertEquals("First report", submission.getNote());
        assertEquals("sha256", submission.getChecksum());
    }

    @Test
    void upload_incrementsVersionAndMarksLate() {
        Fixture fixture = fixture();
        fixture.milestone.setAllowLateSubmission(true);
        stubFixture(fixture);
        when(submissionRepository.findFirstByTeam_IdTeamAndMilesStone_IdMilesStoneOrderByVersionDesc(10L, 5L))
                .thenReturn(Optional.of(SubmistionEntity.builder()
                        .version(2)
                        .status(SubmissionStatusConstain.REVISION_REQUIRED)
                        .build()));
        when(fileStorageService.store(any(), any(), any(), any(), any()))
                .thenReturn(new FileStorageService.StoredFile("relative/file.pdf", "uuid.pdf", "sha256"));
        when(submissionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        submissionService.upload(5L, 10L, null, pdf(), LocalDateTime.of(2026, 9, 16, 8, 0));

        ArgumentCaptor<SubmistionEntity> captor = ArgumentCaptor.forClass(SubmistionEntity.class);
        verify(submissionRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getVersion());
        assertEquals(true, captor.getValue().getIsLate());
    }

    @Test
    void upload_rejectsAfterDeadlineWhenLateSubmissionDisabled() {
        Fixture fixture = fixture();
        fixture.milestone.setAllowLateSubmission(false);
        stubFixture(fixture);

        AppException exception = assertThrows(
                AppException.class,
                () -> submissionService.upload(5L, 10L, null, pdf(), LocalDateTime.of(2026, 9, 16, 8, 0)));

        assertEquals(ErrorCode.SUBMISSION_DEADLINE_PASSED, exception.getErrorCode());
        verify(fileStorageService, never()).store(any(), any(), any(), any(), any());
    }

    @Test
    void upload_rejectsDisallowedFileType() {
        Fixture fixture = fixture();
        stubFixture(fixture);
        MockMultipartFile executable =
                new MockMultipartFile("file", "virus.exe", "application/octet-stream", new byte[] {1});

        AppException exception = assertThrows(
                AppException.class,
                () -> submissionService.upload(5L, 10L, null, executable, LocalDateTime.of(2026, 9, 10, 8, 0)));

        assertEquals(ErrorCode.SUBMISSION_FILE_TYPE_NOT_ALLOWED, exception.getErrorCode());
    }

    @Test
    void upload_rejectsApprovedLatestVersion() {
        Fixture fixture = fixture();
        stubFixture(fixture);
        when(submissionRepository.findFirstByTeam_IdTeamAndMilesStone_IdMilesStoneOrderByVersionDesc(10L, 5L))
                .thenReturn(Optional.of(SubmistionEntity.builder()
                        .version(1)
                        .status(SubmissionStatusConstain.APPROVED)
                        .build()));

        AppException exception = assertThrows(
                AppException.class,
                () -> submissionService.upload(5L, 10L, null, pdf(), LocalDateTime.of(2026, 9, 10, 8, 0)));

        assertEquals(ErrorCode.SUBMISSION_ALREADY_APPROVED, exception.getErrorCode());
        verify(fileStorageService, never()).store(any(), any(), any(), any(), any());
    }

    private void stubFixture(Fixture fixture) {
        when(milestoneRepository.findById(5L)).thenReturn(Optional.of(fixture.milestone));
        when(teamRepository.findWithDetailsByIdTeam(10L)).thenReturn(Optional.of(fixture.team));
        when(studentRepository.findByUserEntity_UserId("user-1")).thenReturn(Optional.of(fixture.student));
    }

    private MockMultipartFile pdf() {
        return new MockMultipartFile("file", "report.pdf", "application/pdf", new byte[] {1, 2, 3});
    }

    private Fixture fixture() {
        DefensePeriodEntity period = DefensePeriodEntity.builder()
                .ID_Defense(2L)
                .periodName("Period")
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .status(DefensePeriodConstain.ONGOING)
                .build();
        MilesStoneEntity milestone = MilesStoneEntity.builder()
                .IdMilesStone(5L)
                .milesStoneName("Progress")
                .milestoneType(MilesStoneTypeConstain.PROGRESS_REPORT)
                .status(MilesStoneStatusConstain.OPEN)
                .startAt(LocalDateTime.of(2026, 9, 1, 8, 0))
                .deadLine(LocalDateTime.of(2026, 9, 15, 23, 59))
                .allowLateSubmission(true)
                .maxFileSize(1000L)
                .allowedFileTypes("pdf,docx")
                .defensePeriod(period)
                .build();
        UserEntity user = UserEntity.builder().userId("user-1").build();
        StudentEntity student = StudentEntity.builder()
                .idStudent("student-1")
                .studentCode("S001")
                .userEntity(user)
                .build();
        TeamEntity team = TeamEntity.builder()
                .idTeam(10L)
                .studentEntities(new ArrayList<>(List.of(student)))
                .build();
        TopicEntity topic = TopicEntity.builder()
                .idTopic(20L)
                .defensePeriod(period)
                .team(team)
                .build();
        team.setTopic(topic);
        student.setTeam(team);
        return new Fixture(milestone, team, student);
    }

    private record Fixture(MilesStoneEntity milestone, TeamEntity team, StudentEntity student) {}
}

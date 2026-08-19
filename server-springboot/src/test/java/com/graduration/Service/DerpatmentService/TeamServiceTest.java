package com.graduration.Service.DerpatmentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.graduration.Constain.TopicStatusConstain;
import com.graduration.DTO.Request.TeamRequest;
import com.graduration.DTO.Response.TeamResponse;
import com.graduration.Repository.StudentRepository;
import com.graduration.Repository.TeamRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.entity.StudentEntity;
import com.graduration.entity.TeamEntity;
import com.graduration.entity.TopicEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.TeamMapper;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {
    @Mock
    TeamRepository teamRepository;

    @Mock
    StudentRepository studentRepository;

    @Mock
    TopicRepository topicRepository;

    @Mock
    TeamMapper teamMapper;

    @InjectMocks
    TeamService teamService;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTeam_createsWithoutStudents() {
        TeamRequest request = TeamRequest.builder().nameTeam(" Team 01 ").build();
        TeamEntity team = TeamEntity.builder().nameTeam("Team 01").build();
        TeamResponse expected = TeamResponse.builder().idTeam(1L).build();
        when(teamMapper.toTeamEntity(request)).thenReturn(team);
        when(teamRepository.save(team)).thenReturn(team);
        when(teamMapper.toTeamResponse(team)).thenReturn(expected);

        TeamResponse actual = teamService.createTeam(request);

        assertSame(expected, actual);
        assertEquals("Team 01", request.getNameTeam());
        assertEquals(0, team.getStudentEntities().size());
    }

    @Test
    void createTeam_rejectsDuplicateName() {
        TeamRequest request = TeamRequest.builder().nameTeam("Team 01").build();
        when(teamRepository.existsByNameTeamIgnoreCase("Team 01")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> teamService.createTeam(request));

        assertEquals(ErrorCode.TEAM_ALREADY_EXISTS, exception.getErrorCode());
        verify(teamRepository, never()).save(any());
    }

    @Test
    void createTeam_linksExistingTopic() {
        TeamRequest request =
                TeamRequest.builder().nameTeam("Team 01").topicId(9L).build();
        TeamEntity team = TeamEntity.builder().nameTeam("Team 01").build();
        TopicEntity topic = TopicEntity.builder()
                .idTopic(9L)
                .status(TopicStatusConstain.APPROVED)
                .build();
        when(topicRepository.findById(9L)).thenReturn(Optional.of(topic));
        when(teamMapper.toTeamEntity(request)).thenReturn(team);
        when(teamRepository.save(team)).thenReturn(team);

        teamService.createTeam(request);

        assertSame(topic, team.getTopic());
    }

    @Test
    void createTeam_rejectsTopicOwnedByAnotherTeam() {
        TeamRequest request =
                TeamRequest.builder().nameTeam("Team 01").topicId(9L).build();
        when(teamRepository.existsByTopic_IdTopic(9L)).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> teamService.createTeam(request));

        assertEquals(ErrorCode.TOPIC_ALREADY_ASSIGNED, exception.getErrorCode());
        verify(teamRepository, never()).save(any());
    }

    @Test
    void getTeam_rejectsMissingTeam() {
        when(teamRepository.findWithDetailsByIdTeam(99L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> teamService.getTeam(99L));

        assertEquals(ErrorCode.TEAM_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getAllTeams_mapsEveryTeam() {
        TeamEntity first = TeamEntity.builder().idTeam(1L).build();
        TeamEntity second = TeamEntity.builder().idTeam(2L).build();
        when(teamRepository.findAllByOrderByIdTeamAsc(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(first, second)));
        when(teamMapper.toTeamResponse(first))
                .thenReturn(TeamResponse.builder().idTeam(1L).build());
        when(teamMapper.toTeamResponse(second))
                .thenReturn(TeamResponse.builder().idTeam(2L).build());

        List<TeamResponse> result = teamService.getAllTeams();

        assertEquals(
                List.of(1L, 2L), result.stream().map(TeamResponse::getIdTeam).toList());
    }

    @Test
    void updateTeam_updatesFieldsAndTopic() {
        TeamEntity team = TeamEntity.builder().idTeam(1L).nameTeam("Old name").build();
        TopicEntity topic = TopicEntity.builder()
                .idTopic(9L)
                .status(TopicStatusConstain.APPROVED)
                .build();
        TeamRequest request =
                TeamRequest.builder().nameTeam(" New name ").topicId(9L).build();
        TeamResponse expected =
                TeamResponse.builder().idTeam(1L).nameTeam("New name").build();
        when(teamRepository.findWithDetailsByIdTeam(1L)).thenReturn(Optional.of(team));
        when(topicRepository.findById(9L)).thenReturn(Optional.of(topic));
        when(teamRepository.save(team)).thenReturn(team);
        when(teamMapper.toTeamResponse(team)).thenReturn(expected);

        TeamResponse result = teamService.updateTeam(1L, request);

        assertSame(expected, result);
        assertSame(topic, team.getTopic());
        assertEquals("New name", request.getNameTeam());
        verify(teamMapper).updateTeam(request, team);
    }

    @Test
    void selectTopic_assignsAvailableTopicAndMarksItRegistered() {
        authenticate("admin-1", "ROLE_ADMIN");
        TeamEntity team = TeamEntity.builder().idTeam(1L).build();
        TopicEntity topic = TopicEntity.builder()
                .idTopic(9L)
                .status(TopicStatusConstain.APPROVED)
                .build();
        TeamResponse expected = TeamResponse.builder().idTeam(1L).topicId(9L).build();
        when(teamRepository.findWithDetailsByIdTeam(1L)).thenReturn(Optional.of(team));
        when(topicRepository.findById(9L)).thenReturn(Optional.of(topic));
        when(teamRepository.save(team)).thenReturn(team);
        when(topicRepository.save(topic)).thenReturn(topic);
        when(teamMapper.toTeamResponse(team)).thenReturn(expected);

        TeamResponse result = teamService.selectTopic(1L, 9L);

        assertSame(expected, result);
        assertSame(topic, team.getTopic());
        assertSame(team, topic.getTeam());
        assertEquals(TopicStatusConstain.REGISTERED, topic.getStatus());
    }

    @Test
    void selectTopic_allowsStudentBelongingToTeam() {
        authenticate("user-1", "ROLE_STUDENT");
        StudentEntity member = StudentEntity.builder()
                .userEntity(com.graduration.entity.UserEntity.builder()
                        .userId("user-1")
                        .build())
                .build();
        TeamEntity team =
                TeamEntity.builder().idTeam(1L).studentEntities(List.of(member)).build();
        TopicEntity topic = TopicEntity.builder()
                .idTopic(9L)
                .status(TopicStatusConstain.APPROVED)
                .build();
        when(teamRepository.findWithDetailsByIdTeam(1L)).thenReturn(Optional.of(team));
        when(topicRepository.findById(9L)).thenReturn(Optional.of(topic));
        when(teamRepository.save(team)).thenReturn(team);

        teamService.selectTopic(1L, 9L);

        assertSame(topic, team.getTopic());
    }

    @Test
    void selectTopic_rejectsUnavailableTopic() {
        authenticate("admin-1", "ROLE_ADMIN");
        TeamEntity team = TeamEntity.builder().idTeam(1L).build();
        TopicEntity topic = TopicEntity.builder()
                .idTopic(9L)
                .status(TopicStatusConstain.DRAFT)
                .build();
        when(teamRepository.findWithDetailsByIdTeam(1L)).thenReturn(Optional.of(team));
        when(topicRepository.findById(9L)).thenReturn(Optional.of(topic));

        AppException exception = assertThrows(AppException.class, () -> teamService.selectTopic(1L, 9L));

        assertEquals(ErrorCode.TOPIC_NOT_AVAILABLE, exception.getErrorCode());
        verify(teamRepository, never()).save(any());
    }

    @Test
    void selectTopic_rejectsStudentOutsideTeam() {
        authenticate("user-2", "ROLE_STUDENT");
        TeamEntity team = TeamEntity.builder().idTeam(1L).build();
        when(teamRepository.findWithDetailsByIdTeam(1L)).thenReturn(Optional.of(team));

        AppException exception = assertThrows(AppException.class, () -> teamService.selectTopic(1L, 9L));

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
        verify(topicRepository, never()).findById(any());
    }

    @Test
    void addStudent_setsOwningSideAndReturnsTeam() {
        TeamEntity team = TeamEntity.builder().idTeam(1L).build();
        StudentEntity student = StudentEntity.builder()
                .idStudent("student-1")
                .studentCode("SV001")
                .build();
        TeamResponse expected = TeamResponse.builder().idTeam(1L).build();
        when(teamRepository.findWithDetailsByIdTeam(1L)).thenReturn(Optional.of(team));
        when(studentRepository.findByStudentCodeIgnoreCase("SV001")).thenReturn(Optional.of(student));
        when(teamMapper.toTeamResponse(team)).thenReturn(expected);

        TeamResponse actual = teamService.addStudent(1L, "SV001");

        assertSame(expected, actual);
        assertSame(team, student.getTeam());
        assertEquals(List.of(student), team.getStudentEntities());
        verify(studentRepository).save(student);
    }

    private void authenticate(String userId, String authority) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        userId, null, List.of(new SimpleGrantedAuthority(authority))));
    }

    @Test
    void addStudents_isAtomicWhenAStudentAlreadyBelongsToAnotherTeam() {
        TeamEntity team = TeamEntity.builder().idTeam(1L).build();
        TeamEntity otherTeam = TeamEntity.builder().idTeam(2L).build();
        StudentEntity first = StudentEntity.builder()
                .idStudent("student-1")
                .studentCode("SV001")
                .build();
        StudentEntity second = StudentEntity.builder()
                .idStudent("student-2")
                .studentCode("SV002")
                .team(otherTeam)
                .build();
        when(teamRepository.findWithDetailsByIdTeam(1L)).thenReturn(Optional.of(team));
        when(studentRepository.findByStudentCodeIgnoreCase("SV001")).thenReturn(Optional.of(first));
        when(studentRepository.findByStudentCodeIgnoreCase("SV002")).thenReturn(Optional.of(second));

        AppException exception =
                assertThrows(AppException.class, () -> teamService.addStudents(1L, Set.of("SV001", "SV002")));

        assertEquals(ErrorCode.STUDENT_ALREADY_IN_TEAM, exception.getErrorCode());
        assertNull(first.getTeam());
        verify(studentRepository, never()).saveAll(any());
    }

    @Test
    void addStudents_addsAllStudentsUsingStudentCodes() {
        TeamEntity team = TeamEntity.builder().idTeam(1L).build();
        StudentEntity first = StudentEntity.builder().studentCode("SV001").build();
        StudentEntity second = StudentEntity.builder().studentCode("SV002").build();
        when(teamRepository.findWithDetailsByIdTeam(1L)).thenReturn(Optional.of(team));
        when(studentRepository.findByStudentCodeIgnoreCase("SV001")).thenReturn(Optional.of(first));
        when(studentRepository.findByStudentCodeIgnoreCase("SV002")).thenReturn(Optional.of(second));

        teamService.addStudents(1L, Set.of(" SV001 ", "SV002"));

        assertSame(team, first.getTeam());
        assertSame(team, second.getTeam());
        assertEquals(2, team.getStudentEntities().size());
        verify(studentRepository).saveAll(any());
    }

    @Test
    void addStudent_rejectsUnknownStudentCode() {
        TeamEntity team = TeamEntity.builder().idTeam(1L).build();
        when(teamRepository.findWithDetailsByIdTeam(1L)).thenReturn(Optional.of(team));
        when(studentRepository.findByStudentCodeIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> teamService.addStudent(1L, "UNKNOWN"));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void removeStudent_detachesStudentByStudentCode() {
        TeamEntity team = TeamEntity.builder().idTeam(1L).build();
        StudentEntity student =
                StudentEntity.builder().studentCode("SV001").team(team).build();
        team.getStudentEntities().add(student);
        when(teamRepository.findWithDetailsByIdTeam(1L)).thenReturn(Optional.of(team));
        when(studentRepository.findByStudentCodeIgnoreCase("sv001")).thenReturn(Optional.of(student));

        teamService.removeStudent(1L, " sv001 ");

        assertNull(student.getTeam());
        assertEquals(0, team.getStudentEntities().size());
        verify(studentRepository).save(student);
    }

    @Test
    void removeStudent_rejectsStudentFromDifferentTeam() {
        TeamEntity team = TeamEntity.builder().idTeam(1L).build();
        StudentEntity student = StudentEntity.builder()
                .studentCode("SV001")
                .team(TeamEntity.builder().idTeam(2L).build())
                .build();
        when(teamRepository.findWithDetailsByIdTeam(1L)).thenReturn(Optional.of(team));
        when(studentRepository.findByStudentCodeIgnoreCase("SV001")).thenReturn(Optional.of(student));

        AppException exception = assertThrows(AppException.class, () -> teamService.removeStudent(1L, "SV001"));

        assertEquals(ErrorCode.INVALID_KEY, exception.getErrorCode());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void importStudents_importsValidRowsAndReportsInvalidRows() throws IOException {
        TeamEntity team = TeamEntity.builder().idTeam(1L).build();
        StudentEntity student = StudentEntity.builder().studentCode("SV001").build();
        TeamResponse.StudentSummary summary =
                TeamResponse.StudentSummary.builder().studentCode("SV001").build();
        when(teamRepository.findWithDetailsByIdTeam(1L)).thenReturn(Optional.of(team));
        when(studentRepository.findByStudentCodeIgnoreCase("SV001")).thenReturn(Optional.of(student));
        when(studentRepository.findByStudentCodeIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());
        when(teamMapper.toStudentSummary(student)).thenReturn(summary);

        TeamService.ImportTeamStudentsResponse result =
                teamService.importStudents(1L, excelFile("studentCode", "SV001", "UNKNOWN"));

        assertEquals(2, result.totalRows());
        assertEquals(1, result.successRows());
        assertEquals(1, result.failedRows());
        assertEquals("SV001", result.importedStudents().get(0).getStudentCode());
        assertEquals("UNKNOWN", result.errors().get(0).studentCode());
    }

    @Test
    void importStudents_rejectsInvalidHeader() throws IOException {
        MockMultipartFile file = excelFile("studentId", "SV001");
        when(teamRepository.findWithDetailsByIdTeam(1L))
                .thenReturn(Optional.of(TeamEntity.builder().idTeam(1L).build()));

        AppException exception = assertThrows(AppException.class, () -> teamService.importStudents(1L, file));

        assertEquals(ErrorCode.INVALID_EXCEL_FILE, exception.getErrorCode());
    }

    @Test
    void deleteTeam_detachesStudentsBeforeDeletingTeam() {
        StudentEntity student = StudentEntity.builder().idStudent("student-1").build();
        TeamEntity team = TeamEntity.builder()
                .idTeam(1L)
                .studentEntities(new ArrayList<>(List.of(student)))
                .build();
        student.setTeam(team);
        when(teamRepository.findWithDetailsByIdTeam(1L)).thenReturn(Optional.of(team));

        teamService.deleteTeam(1L);

        assertNull(student.getTeam());
        assertEquals(0, team.getStudentEntities().size());
        verify(studentRepository).saveAll(any());
        verify(teamRepository).delete(team);
    }

    private MockMultipartFile excelFile(String header, String... studentCodes) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("students");
            sheet.createRow(0).createCell(0).setCellValue(header);
            for (int index = 0; index < studentCodes.length; index++) {
                sheet.createRow(index + 1).createCell(0).setCellValue(studentCodes[index]);
            }
            workbook.write(output);
            return new MockMultipartFile(
                    "file",
                    "team-students.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray());
        }
    }
}

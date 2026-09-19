package com.graduration.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

import com.graduration.Constain.ScoreStatusConstain;
import com.graduration.DTO.Request.ScoreRequest;
import com.graduration.Repository.LectureRepository;
import com.graduration.Repository.ScoreRepository;
import com.graduration.Repository.StudentRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.Service.GradurationService.ScoreService;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.LectureEntity;
import com.graduration.entity.ScoreEntity;
import com.graduration.entity.StudentEntity;
import com.graduration.entity.TeamEntity;
import com.graduration.entity.TopicEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.ScoreMapper;

@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {
    @Mock
    ScoreRepository scoreRepository;

    @Mock
    StudentRepository studentRepository;

    @Mock
    TopicRepository topicRepository;

    @Mock
    LectureRepository lectureRepository;

    @Mock
    ScoreMapper scoreMapper;

    @InjectMocks
    ScoreService scoreService;

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
    void saveDraft_savesDirectScoreForStudent() {
        Fixture fixture = fixture();
        when(studentRepository.findById("student-1")).thenReturn(Optional.of(fixture.student));
        when(topicRepository.findById(9L)).thenReturn(Optional.of(fixture.topic));
        when(lectureRepository.findByUser_UserId("admin"))
                .thenReturn(Optional.of(
                        LectureEntity.builder().lectureId("lecturer-1").build()));
        when(scoreRepository.findStudentTopicScore(
                        "student-1", 9L, "lecturer-1", com.graduration.Constain.ScoreTypeConstain.FINAL))
                .thenReturn(Optional.empty());
        when(scoreRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        scoreService.saveDraft("student-1", 9L, request("8.50"));

        ArgumentCaptor<ScoreEntity> captor = ArgumentCaptor.forClass(ScoreEntity.class);
        verify(scoreRepository).save(captor.capture());
        assertEquals(new BigDecimal("8.50"), captor.getValue().getScore());
        assertEquals("Đánh giá đạt yêu cầu", captor.getValue().getComment());
        assertEquals(ScoreStatusConstain.DRAFT, captor.getValue().getStatus());
    }

    @Test
    void saveDraft_rejectsScoreAboveTen() {
        Fixture fixture = fixture();
        when(studentRepository.findById("student-1")).thenReturn(Optional.of(fixture.student));
        when(topicRepository.findById(9L)).thenReturn(Optional.of(fixture.topic));
        when(lectureRepository.findByUser_UserId("admin"))
                .thenReturn(Optional.of(
                        LectureEntity.builder().lectureId("lecturer-1").build()));
        when(scoreRepository.findStudentTopicScore(
                        "student-1", 9L, "lecturer-1", com.graduration.Constain.ScoreTypeConstain.FINAL))
                .thenReturn(Optional.empty());

        AppException exception =
                assertThrows(AppException.class, () -> scoreService.saveDraft("student-1", 9L, request("10.01")));

        assertEquals(ErrorCode.SCORE_VALUE_INVALID, exception.getErrorCode());
    }

    @Test
    void publish_changesSubmittedScoreToLocked() {
        Fixture fixture = fixture();
        ScoreEntity score = ScoreEntity.builder()
                .id(11L)
                .student(fixture.student)
                .topic(fixture.topic)
                .status(ScoreStatusConstain.SUBMITTED)
                .build();
        when(scoreRepository.findWithRelationsById(11L)).thenReturn(Optional.of(score));
        when(scoreRepository.save(score)).thenReturn(score);

        scoreService.publish(11L);

        assertEquals(ScoreStatusConstain.LOCKED, score.getStatus());
        verify(scoreRepository).save(score);
    }

    private ScoreRequest request(String score) {
        return ScoreRequest.builder()
                .score(new BigDecimal(score))
                .comment("Đánh giá đạt yêu cầu")
                .build();
    }

    private Fixture fixture() {
        DefensePeriodEntity period =
                DefensePeriodEntity.builder().ID_Defense(2L).build();
        TeamEntity team = TeamEntity.builder().idTeam(7L).build();
        StudentEntity student =
                StudentEntity.builder().idStudent("student-1").team(team).build();
        TopicEntity topic = TopicEntity.builder()
                .idTopic(9L)
                .team(team)
                .defensePeriod(period)
                .build();
        return new Fixture(student, topic);
    }

    private record Fixture(StudentEntity student, TopicEntity topic) {}
}

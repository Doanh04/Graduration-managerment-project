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
import com.graduration.Repository.ScoreCriterionRepository;
import com.graduration.Repository.ScoreRepository;
import com.graduration.Repository.StudentRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.Service.GradurationService.ScoreService;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.ScoreCriterionEntity;
import com.graduration.entity.ScoreEntity;
import com.graduration.entity.StudentEntity;
import com.graduration.entity.TeamEntity;
import com.graduration.entity.TopicEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.ScoreMapper;

@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {
    @Mock
    ScoreRepository scoreRepository;

    @Mock
    ScoreCriterionRepository criterionRepository;

    @Mock
    StudentRepository studentRepository;

    @Mock
    TopicRepository topicRepository;

    @Mock
    UserRepository userRepository;

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
    void saveDraft_calculatesWeightedTotalAndCreatesOneScoreSheet() {
        Fixture fixture = fixture();
        when(studentRepository.findById("student-1")).thenReturn(Optional.of(fixture.student));
        when(topicRepository.findById(9L)).thenReturn(Optional.of(fixture.topic));
        when(scoreRepository.findStudentTopicScore("student-1", 9L)).thenReturn(Optional.empty());
        when(userRepository.findById("admin"))
                .thenReturn(Optional.of(UserEntity.builder().userId("admin").build()));
        when(criterionRepository.findActiveByDefensePeriod(2L)).thenReturn(fixture.criteria);
        when(scoreRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        scoreService.saveDraft("student-1", 9L, request("3.00", "5.00"));

        ArgumentCaptor<ScoreEntity> captor = ArgumentCaptor.forClass(ScoreEntity.class);
        verify(scoreRepository).save(captor.capture());
        assertEquals(new BigDecimal("8.00"), captor.getValue().getScore());
        assertEquals(2, captor.getValue().getDetails().size());
        assertEquals(ScoreStatusConstain.DRAFT, captor.getValue().getStatus());
    }

    @Test
    void saveDraft_rejectsMissingCriterion() {
        Fixture fixture = fixture();
        when(studentRepository.findById("student-1")).thenReturn(Optional.of(fixture.student));
        when(topicRepository.findById(9L)).thenReturn(Optional.of(fixture.topic));
        when(scoreRepository.findStudentTopicScore("student-1", 9L)).thenReturn(Optional.empty());
        when(userRepository.findById("admin"))
                .thenReturn(Optional.of(UserEntity.builder().userId("admin").build()));
        when(criterionRepository.findActiveByDefensePeriod(2L)).thenReturn(fixture.criteria);
        ScoreRequest request = ScoreRequest.builder()
                .details(List.of(ScoreRequest.Detail.builder()
                        .criterionId(1L)
                        .score(BigDecimal.ONE)
                        .build()))
                .build();

        AppException exception =
                assertThrows(AppException.class, () -> scoreService.saveDraft("student-1", 9L, request));

        assertEquals(ErrorCode.SCORE_CRITERIA_MISMATCH, exception.getErrorCode());
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
        when(scoreRepository.findWithDetailsById(11L)).thenReturn(Optional.of(score));
        when(scoreRepository.save(score)).thenReturn(score);

        scoreService.publish(11L);

        assertEquals(ScoreStatusConstain.LOCKED, score.getStatus());
        verify(scoreRepository).save(score);
    }

    private ScoreRequest request(String first, String second) {
        return ScoreRequest.builder()
                .details(List.of(
                        ScoreRequest.Detail.builder()
                                .criterionId(1L)
                                .score(new BigDecimal(first))
                                .build(),
                        ScoreRequest.Detail.builder()
                                .criterionId(2L)
                                .score(new BigDecimal(second))
                                .build()))
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
        List<ScoreCriterionEntity> criteria = List.of(
                ScoreCriterionEntity.builder()
                        .criterionId(1L)
                        .maxScore(new BigDecimal("4.00"))
                        .weight(new BigDecimal("40.00"))
                        .active(true)
                        .build(),
                ScoreCriterionEntity.builder()
                        .criterionId(2L)
                        .maxScore(new BigDecimal("6.00"))
                        .weight(new BigDecimal("60.00"))
                        .active(true)
                        .build());
        return new Fixture(student, topic, criteria);
    }

    private record Fixture(StudentEntity student, TopicEntity topic, List<ScoreCriterionEntity> criteria) {}
}

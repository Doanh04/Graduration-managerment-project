package com.graduration.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.Constain.ScoreTypeConstain;
import com.graduration.DTO.Request.ScoreCriterionRequest;
import com.graduration.Repository.DefensePeriodRepository;
import com.graduration.Repository.ScoreCriterionRepository;
import com.graduration.Service.GradurationService.ScoreCriterionService;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.ScoreCriterionEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.ScoreMapper;

@ExtendWith(MockitoExtension.class)
class ScoreCriterionServiceTest {
    @Mock
    ScoreCriterionRepository criterionRepository;

    @Mock
    DefensePeriodRepository defensePeriodRepository;

    @Mock
    ScoreMapper scoreMapper;

    @InjectMocks
    ScoreCriterionService criterionService;

    @Test
    void create_normalizesCodeAndUsesFinalScoreType() {
        DefensePeriodEntity period = DefensePeriodEntity.builder()
                .ID_Defense(2L)
                .status(DefensePeriodConstain.ONGOING)
                .build();
        when(defensePeriodRepository.findById(2L)).thenReturn(Optional.of(period));
        when(criterionRepository.existsDuplicateCode("TECH", 2L, null)).thenReturn(false);
        when(criterionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        criterionService.create(2L, request(" tech ", new BigDecimal("10"), new BigDecimal("40")));

        ArgumentCaptor<ScoreCriterionEntity> captor = ArgumentCaptor.forClass(ScoreCriterionEntity.class);
        verify(criterionRepository).save(captor.capture());
        assertEquals("TECH", captor.getValue().getCriterionCode());
        assertEquals(ScoreTypeConstain.FINAL, captor.getValue().getScoreType());
    }

    @Test
    void create_rejectsFinishedDefensePeriod() {
        DefensePeriodEntity period = DefensePeriodEntity.builder()
                .ID_Defense(2L)
                .status(DefensePeriodConstain.FINISHED)
                .build();
        when(defensePeriodRepository.findById(2L)).thenReturn(Optional.of(period));

        AppException exception = assertThrows(
                AppException.class,
                () -> criterionService.create(2L, request("TECH", BigDecimal.TEN, new BigDecimal("40"))));

        assertEquals(ErrorCode.DEFENSE_PERIOD_FINISHED, exception.getErrorCode());
    }

    @Test
    void create_rejectsInvalidMaximumScoreWithoutControllerValidation() {
        DefensePeriodEntity period = DefensePeriodEntity.builder()
                .ID_Defense(2L)
                .status(DefensePeriodConstain.ONGOING)
                .build();
        when(defensePeriodRepository.findById(2L)).thenReturn(Optional.of(period));

        AppException exception = assertThrows(
                AppException.class, () -> criterionService.create(2L, request("TECH", null, new BigDecimal("40"))));

        assertEquals(ErrorCode.SCORE_CRITERION_MAX_SCORE_INVALID, exception.getErrorCode());
    }

    private ScoreCriterionRequest request(String code, BigDecimal maxScore, BigDecimal weight) {
        return ScoreCriterionRequest.builder()
                .criterionCode(code)
                .criterionName("Technology")
                .maxScore(maxScore)
                .weight(weight)
                .active(true)
                .build();
    }
}

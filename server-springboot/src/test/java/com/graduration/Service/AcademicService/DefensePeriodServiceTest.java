package com.graduration.Service.AcademicService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.DTO.Request.DefensePeriodRequest;
import com.graduration.DTO.Response.DefensePeriodResponse;
import com.graduration.Repository.AcademicYearRepository;
import com.graduration.Repository.DefensePeriodRepository;
import com.graduration.entity.AcademicYearEntity;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.TopicEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.DefensePeriodMapper;

@ExtendWith(MockitoExtension.class)
class DefensePeriodServiceTest {
    @Mock
    DefensePeriodRepository defensePeriodRepository;

    @Mock
    AcademicYearRepository academicYearRepository;

    @Mock
    DefensePeriodMapper defensePeriodMapper;

    @InjectMocks
    DefensePeriodService defensePeriodService;

    @Test
    void createDefensePeriod_linksAcademicYearAndSaves() {
        DefensePeriodRequest request = validRequest();
        AcademicYearEntity academicYear =
                AcademicYearEntity.builder().academicId(1).build();
        DefensePeriodEntity entity = entity(request.getEndDate(), DefensePeriodConstain.PENDING);
        when(academicYearRepository.findById(1)).thenReturn(Optional.of(academicYear));
        when(defensePeriodMapper.toDefensePeriodEntity(request)).thenReturn(entity);
        when(defensePeriodRepository.save(entity)).thenReturn(entity);
        when(defensePeriodMapper.toDefensePeriodResponse(entity)).thenReturn(response());

        DefensePeriodResponse result = defensePeriodService.createDefensePeriod(request);

        assertEquals(10L, result.getDefensePeriodId());
        assertSame(academicYear, entity.getAcademicYear());
        verify(defensePeriodRepository).save(entity);
    }

    @Test
    void createDefensePeriod_forcesFinishedWhenEndDatePassed() {
        DefensePeriodRequest request = validRequest();
        request.setStartDate(LocalDate.now().minusDays(10));
        request.setEndDate(LocalDate.now().minusDays(1));
        AcademicYearEntity academicYear =
                AcademicYearEntity.builder().academicId(1).build();
        DefensePeriodEntity entity = entity(request.getEndDate(), DefensePeriodConstain.ONGOING);
        when(academicYearRepository.findById(1)).thenReturn(Optional.of(academicYear));
        when(defensePeriodMapper.toDefensePeriodEntity(request)).thenReturn(entity);
        when(defensePeriodRepository.save(entity)).thenReturn(entity);

        defensePeriodService.createDefensePeriod(request);

        assertEquals(DefensePeriodConstain.FINISHED, entity.getStatus());
    }

    @Test
    void createDefensePeriod_rejectsEndDateBeforeStartDate() {
        DefensePeriodRequest request = validRequest();
        request.setEndDate(request.getStartDate().minusDays(1));

        AppException exception =
                assertThrows(AppException.class, () -> defensePeriodService.createDefensePeriod(request));

        assertEquals(ErrorCode.DEFENSE_PERIOD_INVALID_DATE, exception.getErrorCode());
        verify(defensePeriodRepository, never()).save(any());
    }

    @Test
    void createDefensePeriod_rejectsDuplicateNameInAcademicYear() {
        DefensePeriodRequest request = validRequest();
        when(academicYearRepository.findById(1))
                .thenReturn(
                        Optional.of(AcademicYearEntity.builder().academicId(1).build()));
        when(defensePeriodRepository.existsByPeriodNameIgnoreCaseAndAcademicYear_AcademicId("Period 1", 1))
                .thenReturn(true);

        AppException exception =
                assertThrows(AppException.class, () -> defensePeriodService.createDefensePeriod(request));

        assertEquals(ErrorCode.DEFENSE_PERIOD_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    void getDefensePeriod_updatesExpiredStatusBeforeReturning() {
        DefensePeriodEntity entity = entity(LocalDate.now().minusDays(1), DefensePeriodConstain.ONGOING);
        when(defensePeriodRepository.findById(10L)).thenReturn(Optional.of(entity));
        when(defensePeriodMapper.toDefensePeriodResponse(entity)).thenReturn(response());

        defensePeriodService.getDefensePeriod(10L);

        assertEquals(DefensePeriodConstain.FINISHED, entity.getStatus());
        verify(defensePeriodRepository).save(entity);
    }

    @Test
    void getAllDefensePeriods_finishesExpiredRowsThenMapsResults() {
        DefensePeriodEntity entity = entity(LocalDate.now().plusDays(1), DefensePeriodConstain.ONGOING);
        when(defensePeriodRepository.markExpiredPeriodsFinished(any(), eqFinished()))
                .thenReturn(2);
        when(defensePeriodRepository.findAllByOrderByStartDateDesc(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(entity)));
        when(defensePeriodMapper.toDefensePeriodResponse(entity)).thenReturn(response());

        List<DefensePeriodResponse> result = defensePeriodService.getAllDefensePeriods();

        assertEquals(1, result.size());
        verify(defensePeriodRepository).markExpiredPeriodsFinished(any(LocalDate.class), eqFinished());
    }

    @Test
    void updateDefensePeriod_updatesMappedFieldsAndAcademicYear() {
        DefensePeriodRequest request = validRequest();
        DefensePeriodEntity entity = entity(request.getEndDate(), DefensePeriodConstain.PENDING);
        AcademicYearEntity academicYear =
                AcademicYearEntity.builder().academicId(1).build();
        when(defensePeriodRepository.findById(10L)).thenReturn(Optional.of(entity));
        when(academicYearRepository.findById(1)).thenReturn(Optional.of(academicYear));
        when(defensePeriodRepository.save(entity)).thenReturn(entity);
        when(defensePeriodMapper.toDefensePeriodResponse(entity)).thenReturn(response());

        defensePeriodService.updateDefensePeriod(10L, request);

        verify(defensePeriodMapper).updateDefensePeriod(request, entity);
        assertSame(academicYear, entity.getAcademicYear());
    }

    @Test
    void deleteDefensePeriod_deletesUnusedPeriod() {
        DefensePeriodEntity entity = entity(LocalDate.now().plusDays(1), DefensePeriodConstain.PENDING);
        when(defensePeriodRepository.findById(10L)).thenReturn(Optional.of(entity));

        defensePeriodService.deleteDefensePeriod(10L);

        verify(defensePeriodRepository).delete(entity);
    }

    @Test
    void deleteDefensePeriod_rejectsPeriodContainingTopic() {
        DefensePeriodEntity entity = entity(LocalDate.now().plusDays(1), DefensePeriodConstain.PENDING);
        entity.getTopic().add(new TopicEntity());
        when(defensePeriodRepository.findById(10L)).thenReturn(Optional.of(entity));

        AppException exception = assertThrows(AppException.class, () -> defensePeriodService.deleteDefensePeriod(10L));

        assertEquals(ErrorCode.DEFENSE_PERIOD_IN_USE, exception.getErrorCode());
        verify(defensePeriodRepository, never()).delete(any());
    }

    @Test
    void scheduledJob_marksAllExpiredPeriodsFinished() {
        when(defensePeriodRepository.markExpiredPeriodsFinished(any(), eqFinished()))
                .thenReturn(3);

        int updatedRows = defensePeriodService.finishExpiredPeriods();

        assertEquals(3, updatedRows);
    }

    private DefensePeriodRequest validRequest() {
        return DefensePeriodRequest.builder()
                .periodName("Period 1")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .projectType("Graduation")
                .status(DefensePeriodConstain.PENDING)
                .academicId(1)
                .build();
    }

    private DefensePeriodEntity entity(LocalDate endDate, DefensePeriodConstain status) {
        return DefensePeriodEntity.builder()
                .ID_Defense(10L)
                .periodName("Period 1")
                .startDate(endDate.minusDays(10))
                .endDate(endDate)
                .status(status)
                .build();
    }

    private DefensePeriodResponse response() {
        return DefensePeriodResponse.builder()
                .defensePeriodId(10L)
                .periodName("Period 1")
                .status(DefensePeriodConstain.PENDING)
                .build();
    }

    private DefensePeriodConstain eqFinished() {
        return org.mockito.ArgumentMatchers.eq(DefensePeriodConstain.FINISHED);
    }
}

package com.graduration.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.Constain.MilesStoneStatusConstain;
import com.graduration.Constain.MilesStoneTypeConstain;
import com.graduration.DTO.Request.CreateMilestoneRequest;
import com.graduration.Repository.DefensePeriodRepository;
import com.graduration.Repository.MilestoneRepository;
import com.graduration.Service.GradurationService.MilestoneService;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.MilesStoneEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.MilestoneMapper;

@ExtendWith(MockitoExtension.class)
class MilestoneServiceTest {
    @Mock
    MilestoneRepository milestoneRepository;

    @Mock
    DefensePeriodRepository defensePeriodRepository;

    @Mock
    MilestoneMapper milestoneMapper;

    @InjectMocks
    MilestoneService milestoneService;

    @Test
    void createMilestone_linksPeriodAndStartsAsDraft() {
        DefensePeriodEntity period = period(DefensePeriodConstain.ONGOING);
        when(defensePeriodRepository.findById(10L)).thenReturn(Optional.of(period));
        when(milestoneRepository.save(any(MilesStoneEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        milestoneService.createMilestone(10L, request());

        org.mockito.ArgumentCaptor<MilesStoneEntity> captor =
                org.mockito.ArgumentCaptor.forClass(MilesStoneEntity.class);
        verify(milestoneRepository).save(captor.capture());
        MilesStoneEntity milestone = captor.getValue();
        assertSame(period, milestone.getDefensePeriod());
        assertEquals(MilesStoneStatusConstain.DRAFT, milestone.getStatus());
        assertEquals("pdf,docx", milestone.getAllowedFileTypes());
    }

    @Test
    void createMilestone_rejectsInvalidDateRange() {
        DefensePeriodEntity period = period(DefensePeriodConstain.ONGOING);
        CreateMilestoneRequest request = request();
        request.setStartAt(request.getDeadline());
        when(defensePeriodRepository.findById(10L)).thenReturn(Optional.of(period));

        AppException exception = assertThrows(AppException.class, () -> milestoneService.createMilestone(10L, request));

        assertEquals(ErrorCode.MILESTONE_INVALID_DATE, exception.getErrorCode());
        verify(milestoneRepository, never()).save(any());
    }

    @Test
    void createMilestone_rejectsTimeOutsideDefensePeriod() {
        DefensePeriodEntity period = period(DefensePeriodConstain.ONGOING);
        CreateMilestoneRequest request = request();
        request.setDeadline(LocalDateTime.of(2027, 1, 1, 0, 0));
        when(defensePeriodRepository.findById(10L)).thenReturn(Optional.of(period));

        AppException exception = assertThrows(AppException.class, () -> milestoneService.createMilestone(10L, request));

        assertEquals(ErrorCode.MILESTONE_OUTSIDE_DEFENSE_PERIOD, exception.getErrorCode());
    }

    @Test
    void openMilestone_changesDraftToOpen() {
        MilesStoneEntity milestone = milestone(MilesStoneStatusConstain.DRAFT);
        when(milestoneRepository.findById(5L)).thenReturn(Optional.of(milestone));
        when(milestoneRepository.save(milestone)).thenReturn(milestone);

        milestoneService.openMilestone(5L);

        assertEquals(MilesStoneStatusConstain.OPEN, milestone.getStatus());
    }

    @Test
    void closeMilestone_requiresOpenStatus() {
        MilesStoneEntity milestone = milestone(MilesStoneStatusConstain.DRAFT);
        when(milestoneRepository.findById(5L)).thenReturn(Optional.of(milestone));

        AppException exception = assertThrows(AppException.class, () -> milestoneService.closeMilestone(5L));

        assertEquals(ErrorCode.MILESTONE_OPERATION_NOT_ALLOWED, exception.getErrorCode());
    }

    @Test
    void deleteMilestone_rejectsMilestoneContainingSubmission() {
        MilesStoneEntity milestone = milestone(MilesStoneStatusConstain.DRAFT);
        milestone.getSubmistion().add(new com.graduration.entity.SubmistionEntity());
        when(milestoneRepository.findById(5L)).thenReturn(Optional.of(milestone));

        AppException exception = assertThrows(AppException.class, () -> milestoneService.deleteMilestone(5L));

        assertEquals(ErrorCode.MILESTONE_IN_USE, exception.getErrorCode());
        verify(milestoneRepository, never()).delete(any(MilesStoneEntity.class));
    }

    private CreateMilestoneRequest request() {
        return CreateMilestoneRequest.builder()
                .milestoneName(" Progress report ")
                .description("First report")
                .startAt(LocalDateTime.of(2026, 9, 1, 8, 0))
                .deadline(LocalDateTime.of(2026, 9, 15, 23, 59))
                .milestoneType(MilesStoneTypeConstain.PROGRESS_REPORT)
                .allowedFileTypes(".PDF, docx, pdf")
                .build();
    }

    private MilesStoneEntity milestone(MilesStoneStatusConstain status) {
        return MilesStoneEntity.builder()
                .IdMilesStone(5L)
                .milesStoneName("Progress report")
                .startAt(LocalDateTime.of(2026, 9, 1, 8, 0))
                .deadLine(LocalDateTime.of(2026, 9, 15, 23, 59))
                .milestoneType(MilesStoneTypeConstain.PROGRESS_REPORT)
                .status(status)
                .defensePeriod(period(DefensePeriodConstain.ONGOING))
                .submistion(new ArrayList<>())
                .build();
    }

    private DefensePeriodEntity period(DefensePeriodConstain status) {
        return DefensePeriodEntity.builder()
                .ID_Defense(10L)
                .periodName("Period 1")
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .status(status)
                .build();
    }
}

package com.graduration.Service.AcademicService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.graduration.DTO.Request.AcademicYearRequest;
import com.graduration.DTO.Response.AcademicYearResponse;
import com.graduration.Repository.AcademicYearRepository;
import com.graduration.entity.AcademicYearEntity;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.AcademicYearMapper;

@ExtendWith(MockitoExtension.class)
class AcademicYearServiceTest {
    @Mock
    AcademicYearRepository academicYearRepository;

    @Mock
    AcademicYearMapper academicYearMapper;

    @InjectMocks
    AcademicYearService academicYearService;

    @Test
    void createAcademicYear_normalizesAndSaves() {
        AcademicYearRequest request = AcademicYearRequest.builder()
                .academicYear(" 2025 / 2026 ")
                .description(" First year ")
                .build();
        AcademicYearEntity entity =
                AcademicYearEntity.builder().academicYear("2025-2026").build();
        AcademicYearResponse expected = response();
        when(academicYearMapper.toAcademicYearEntity(request)).thenReturn(entity);
        when(academicYearRepository.save(entity)).thenReturn(entity);
        when(academicYearMapper.toAcademicYearResponse(entity)).thenReturn(expected);

        AcademicYearResponse result = academicYearService.createAcademicYear(request);

        assertSame(expected, result);
        assertEquals("2025-2026", request.getAcademicYear());
        assertEquals("First year", request.getDescription());
    }

    @Test
    void createAcademicYear_rejectsDuplicate() {
        AcademicYearRequest request =
                AcademicYearRequest.builder().academicYear("2025-2026").build();
        when(academicYearRepository.existsByAcademicYearIgnoreCase("2025-2026")).thenReturn(true);

        AppException exception =
                assertThrows(AppException.class, () -> academicYearService.createAcademicYear(request));

        assertEquals(ErrorCode.ACADEMIC_YEAR_ALREADY_EXISTS, exception.getErrorCode());
        verify(academicYearRepository, never()).save(any());
    }

    @Test
    void createAcademicYear_rejectsInvalidYearRange() {
        AcademicYearRequest request =
                AcademicYearRequest.builder().academicYear("2026-2025").build();

        AppException exception =
                assertThrows(AppException.class, () -> academicYearService.createAcademicYear(request));

        assertEquals(ErrorCode.ACADEMIC_YEAR_INVALID, exception.getErrorCode());
    }

    @Test
    void getAcademicYear_returnsMappedEntity() {
        AcademicYearEntity entity = AcademicYearEntity.builder().academicId(1).build();
        when(academicYearRepository.findById(1)).thenReturn(Optional.of(entity));
        when(academicYearMapper.toAcademicYearResponse(entity)).thenReturn(response());

        AcademicYearResponse result = academicYearService.getAcademicYear(1);

        assertEquals(1, result.getAcademicId());
    }

    @Test
    void getAcademicYearByName_normalizesSlashFormat() {
        AcademicYearEntity entity = AcademicYearEntity.builder().academicId(1).build();
        when(academicYearRepository.findByAcademicYearIgnoreCase("2025-2026")).thenReturn(Optional.of(entity));
        when(academicYearMapper.toAcademicYearResponse(entity)).thenReturn(response());

        academicYearService.getAcademicYearByName("2025 / 2026");

        verify(academicYearRepository).findByAcademicYearIgnoreCase("2025-2026");
    }

    @Test
    void getAllAcademicYears_mapsAllEntities() {
        AcademicYearEntity first = AcademicYearEntity.builder().academicId(1).build();
        AcademicYearEntity second = AcademicYearEntity.builder().academicId(2).build();
        when(academicYearRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(first, second)));
        when(academicYearMapper.toAcademicYearResponse(first)).thenReturn(response());
        when(academicYearMapper.toAcademicYearResponse(second))
                .thenReturn(AcademicYearResponse.builder().academicId(2).build());

        List<AcademicYearResponse> result = academicYearService.getAllAcademicYears();

        assertEquals(
                List.of(1, 2),
                result.stream().map(AcademicYearResponse::getAcademicId).toList());
    }

    @Test
    void updateAcademicYear_updatesExistingEntity() {
        AcademicYearEntity entity = AcademicYearEntity.builder()
                .academicId(1)
                .academicYear("2024-2025")
                .build();
        AcademicYearRequest request =
                AcademicYearRequest.builder().academicYear("2025-2026").build();
        when(academicYearRepository.findById(1)).thenReturn(Optional.of(entity));
        when(academicYearRepository.save(entity)).thenReturn(entity);
        when(academicYearMapper.toAcademicYearResponse(entity)).thenReturn(response());

        academicYearService.updateAcademicYear(1, request);

        verify(academicYearMapper).updateAcademicYear(request, entity);
        verify(academicYearRepository).save(entity);
    }

    @Test
    void deleteAcademicYear_deletesEmptyYear() {
        AcademicYearEntity entity = AcademicYearEntity.builder().academicId(1).build();
        when(academicYearRepository.findById(1)).thenReturn(Optional.of(entity));

        academicYearService.deleteAcademicYear(1);

        verify(academicYearRepository).delete(entity);
    }

    @Test
    void deleteAcademicYear_rejectsYearContainingDefenseData() {
        AcademicYearEntity entity = AcademicYearEntity.builder()
                .academicId(1)
                .defensePeriod(Set.of(new DefensePeriodEntity()))
                .build();
        when(academicYearRepository.findById(1)).thenReturn(Optional.of(entity));

        AppException exception = assertThrows(AppException.class, () -> academicYearService.deleteAcademicYear(1));

        assertEquals(ErrorCode.ACADEMIC_YEAR_IN_USE, exception.getErrorCode());
        verify(academicYearRepository, never()).delete(any());
    }

    private AcademicYearResponse response() {
        return AcademicYearResponse.builder()
                .academicId(1)
                .academicYear("2025-2026")
                .build();
    }
}

package com.graduration.Service.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.graduration.DTO.Request.MajorRequest;
import com.graduration.DTO.Response.MajorResponse;
import com.graduration.Repository.MajorRepository;
import com.graduration.Service.DerpatmentService.MajorService;
import com.graduration.entity.MajorEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.MajorMapper;

@ExtendWith(MockitoExtension.class)
class MajorServiceTest {
    @Mock
    MajorRepository majorRepository;

    @Mock
    MajorMapper majorMapper;

    @InjectMocks
    MajorService majorService;

    @Test
    void createMajor_normalizesMapsAndSavesMajor() {
        MajorRequest request = MajorRequest.builder()
                .majorName("  Information Technology  ")
                .description("  Software and systems  ")
                .build();
        MajorEntity mappedMajor = MajorEntity.builder()
                .majorName("Information Technology")
                .description("Software and systems")
                .build();
        MajorEntity savedMajor = MajorEntity.builder()
                .majorId(1L)
                .majorName("Information Technology")
                .description("Software and systems")
                .build();
        MajorResponse expected = MajorResponse.builder()
                .majorName("Information Technology")
                .description("Software and systems")
                .build();

        when(majorMapper.toMajorEntity(request)).thenReturn(mappedMajor);
        when(majorRepository.save(mappedMajor)).thenReturn(savedMajor);
        when(majorMapper.toMajorResponse(savedMajor)).thenReturn(expected);

        MajorResponse actual = majorService.createMajor(request);

        assertSame(expected, actual);
        assertEquals("Information Technology", request.getMajorName());
        assertEquals("Software and systems", request.getDescription());
        verify(majorRepository).save(mappedMajor);
    }

    @Test
    void createMajor_convertsBlankDescriptionToNull() {
        MajorRequest request = MajorRequest.builder()
                .majorName("Computer Science")
                .description("   ")
                .build();
        MajorEntity major = MajorEntity.builder().majorName("Computer Science").build();

        when(majorMapper.toMajorEntity(request)).thenReturn(major);
        when(majorRepository.save(major)).thenReturn(major);

        majorService.createMajor(request);

        assertNull(request.getDescription());
    }

    @Test
    void createMajor_rejectsBlankMajorName() {
        MajorRequest request = MajorRequest.builder().majorName("   ").build();

        AppException exception = assertThrows(AppException.class, () -> majorService.createMajor(request));

        assertEquals(ErrorCode.INVALID_KEY, exception.getErrorCode());
        verify(majorRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createMajor_rejectsDuplicateNameIgnoringCase() {
        MajorRequest request =
                MajorRequest.builder().majorName("computer science").build();
        when(majorRepository.existsByMajorNameIgnoreCase("computer science")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> majorService.createMajor(request));

        assertEquals(ErrorCode.INVALID_KEY, exception.getErrorCode());
        verify(majorMapper, never()).toMajorEntity(request);
        verify(majorRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getMajor_returnsMappedMajor() {
        MajorEntity major =
                MajorEntity.builder().majorId(1L).majorName("Computer Science").build();
        MajorResponse expected =
                MajorResponse.builder().majorName("Computer Science").build();
        when(majorRepository.findById(1L)).thenReturn(Optional.of(major));
        when(majorMapper.toMajorResponse(major)).thenReturn(expected);

        MajorResponse actual = majorService.getMajor(1L);

        assertSame(expected, actual);
    }

    @Test
    void getMajor_rejectsInvalidId() {
        AppException exception = assertThrows(AppException.class, () -> majorService.getMajor(0L));

        assertEquals(ErrorCode.INVALID_KEY, exception.getErrorCode());
        verify(majorRepository).findById(0L);
    }

    @Test
    void getMajor_throwsWhenMajorDoesNotExist() {
        when(majorRepository.findById(99L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> majorService.getMajor(99L));

        assertEquals(ErrorCode.INVALID_KEY, exception.getErrorCode());
    }

    @Test
    void getAllMajors_returnsMappedMajors() {
        MajorEntity first =
                MajorEntity.builder().majorId(1L).majorName("Computer Science").build();
        MajorEntity second =
                MajorEntity.builder().majorId(2L).majorName("Business").build();
        MajorResponse firstResponse =
                MajorResponse.builder().majorName("Computer Science").build();
        MajorResponse secondResponse =
                MajorResponse.builder().majorName("Business").build();
        when(majorRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(first, second)));
        when(majorMapper.toMajorResponse(first)).thenReturn(firstResponse);
        when(majorMapper.toMajorResponse(second)).thenReturn(secondResponse);

        List<MajorResponse> responses = majorService.getAllMajors();

        assertEquals(2, responses.size());
        assertSame(firstResponse, responses.get(0));
        assertSame(secondResponse, responses.get(1));
    }

    @Test
    void updateMajor_updatesFieldsAndPreservesEntityIdentity() {
        MajorRequest request = MajorRequest.builder()
                .majorName("  Data Science  ")
                .description("  Analytics  ")
                .build();
        MajorEntity existing = MajorEntity.builder()
                .majorId(1L)
                .majorName("Computer Science")
                .description("Old description")
                .build();
        MajorEntity mapped = MajorEntity.builder()
                .majorName("Data Science")
                .description("Analytics")
                .build();
        MajorResponse expected = MajorResponse.builder()
                .majorName("Data Science")
                .description("Analytics")
                .build();
        when(majorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(majorRepository.existsByMajorNameIgnoreCaseAndMajorIdNot("Data Science", 1L))
                .thenReturn(false);
        when(majorMapper.toMajorEntity(request)).thenReturn(mapped);
        when(majorRepository.save(existing)).thenReturn(existing);
        when(majorMapper.toMajorResponse(existing)).thenReturn(expected);

        MajorResponse actual = majorService.updateMajor(1L, request);

        assertSame(expected, actual);
        assertEquals(1L, existing.getMajorId());
        assertEquals("Data Science", existing.getMajorName());
        assertEquals("Analytics", existing.getDescription());
        verify(majorRepository).save(existing);
    }

    @Test
    void updateMajor_rejectsNameOwnedByAnotherMajor() {
        MajorRequest request = MajorRequest.builder().majorName("Business").build();
        MajorEntity current =
                MajorEntity.builder().majorId(1L).majorName("Computer Science").build();
        when(majorRepository.findById(1L)).thenReturn(Optional.of(current));
        when(majorRepository.existsByMajorNameIgnoreCaseAndMajorIdNot("Business", 1L))
                .thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> majorService.updateMajor(1L, request));

        assertEquals(ErrorCode.INVALID_KEY, exception.getErrorCode());
        verify(majorRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteMajor_deletesExistingMajor() {
        MajorEntity major =
                MajorEntity.builder().majorId(1L).majorName("Computer Science").build();
        when(majorRepository.findById(1L)).thenReturn(Optional.of(major));

        majorService.deleteMajor(1L);

        verify(majorRepository).delete(major);
    }
}

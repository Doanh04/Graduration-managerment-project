package com.graduration.Service.DerpatmentService;

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

import com.graduration.DTO.Request.ClassRequest;
import com.graduration.DTO.Response.ClassResponse;
import com.graduration.Repository.ClassRepository;
import com.graduration.Repository.MajorRepository;
import com.graduration.entity.ClassEntity;
import com.graduration.entity.MajorEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.ClassMapper;

@ExtendWith(MockitoExtension.class)
class ClassServiceTest {
    @Mock
    ClassRepository classRepository;

    @Mock
    MajorRepository majorRepository;

    @Mock
    ClassMapper classMapper;

    @InjectMocks
    ClassService classService;

    @Test
    void createClass_normalizesMapsMajorAndSavesClass() {
        ClassRequest request = request();
        request.setClassCode("  CNTT01  ");
        request.setNameClass("  Information Technology 1  ");
        request.setMajorId("  1  ");
        request.setDescription("  First class  ");
        MajorEntity major = MajorEntity.builder().majorId(1L).build();
        ClassEntity mapped = ClassEntity.builder()
                .classCode("CNTT01")
                .className("Information Technology 1")
                .description("First class")
                .build();
        ClassEntity saved = ClassEntity.builder()
                .classId(10L)
                .classCode("CNTT01")
                .className("Information Technology 1")
                .major(major)
                .build();
        ClassResponse expected = response();

        when(majorRepository.findById(1L)).thenReturn(Optional.of(major));
        when(classMapper.toClassEntity(request)).thenReturn(mapped);
        when(classRepository.save(mapped)).thenReturn(saved);
        when(classMapper.toClassResponse(saved)).thenReturn(expected);

        ClassResponse actual = classService.createClass(request);

        assertSame(expected, actual);
        assertEquals("CNTT01", request.getClassCode());
        assertEquals("Information Technology 1", request.getNameClass());
        assertEquals("1", request.getMajorId());
        assertEquals("First class", request.getDescription());
        assertSame(major, mapped.getMajor());
        verify(classRepository).save(mapped);
    }

    @Test
    void createClass_convertsBlankDescriptionToNull() {
        ClassRequest request = request();
        request.setDescription("   ");
        MajorEntity major = MajorEntity.builder().majorId(1L).build();
        ClassEntity mapped = new ClassEntity();

        when(majorRepository.findById(1L)).thenReturn(Optional.of(major));
        when(classMapper.toClassEntity(request)).thenReturn(mapped);
        when(classRepository.save(mapped)).thenReturn(mapped);

        classService.createClass(request);

        assertNull(request.getDescription());
    }

    @Test
    void createClass_rejectsDuplicateClassCode() {
        ClassRequest request = request();
        when(classRepository.existsByClassCodeIgnoreCase("CNTT01")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> classService.createClass(request));

        assertEquals(ErrorCode.INVALID_KEY, exception.getErrorCode());
        verify(majorRepository, never()).findById(any());
        verify(classRepository, never()).save(any());
    }

    @Test
    void createClass_rejectsDuplicateClassName() {
        ClassRequest request = request();
        when(classRepository.existsByClassNameIgnoreCase("Information Technology 1"))
                .thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> classService.createClass(request));

        assertEquals(ErrorCode.INVALID_KEY, exception.getErrorCode());
        verify(classRepository, never()).save(any());
    }

    @Test
    void createClass_rejectsInvalidMajorId() {
        ClassRequest request = request();
        request.setMajorId("invalid");

        AppException exception = assertThrows(AppException.class, () -> classService.createClass(request));

        assertEquals(ErrorCode.INVALID_KEY, exception.getErrorCode());
        verify(majorRepository, never()).findById(any());
    }

    @Test
    void createClass_rejectsMissingMajor() {
        ClassRequest request = request();
        when(majorRepository.findById(1L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> classService.createClass(request));

        assertEquals(ErrorCode.INVALID_KEY, exception.getErrorCode());
        verify(classRepository, never()).save(any());
    }

    @Test
    void getClass_returnsMappedClass() {
        ClassEntity classEntity =
                ClassEntity.builder().classId(10L).classCode("CNTT01").build();
        ClassResponse expected = response();
        when(classRepository.findByClassCodeIgnoreCase("CNTT01")).thenReturn(Optional.of(classEntity));
        when(classMapper.toClassResponse(classEntity)).thenReturn(expected);

        ClassResponse actual = classService.getClass(" CNTT01 ");

        assertSame(expected, actual);
        verify(classRepository).findByClassCodeIgnoreCase("CNTT01");
    }

    @Test
    void getClass_rejectsMissingClass() {
        when(classRepository.findByClassCodeIgnoreCase("MISSING")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> classService.getClass("MISSING"));

        assertEquals(ErrorCode.INVALID_KEY, exception.getErrorCode());
    }

    @Test
    void getAllClasses_returnsMappedClasses() {
        ClassEntity first = ClassEntity.builder().classId(10L).build();
        ClassEntity second = ClassEntity.builder().classId(11L).build();
        ClassResponse firstResponse = response();
        ClassResponse secondResponse =
                ClassResponse.builder().idClass(11L).classCode("CNTT02").build();
        when(classRepository.findAll()).thenReturn(List.of(first, second));
        when(classMapper.toClassResponse(first)).thenReturn(firstResponse);
        when(classMapper.toClassResponse(second)).thenReturn(secondResponse);

        List<ClassResponse> responses = classService.getAllClasses();

        assertEquals(2, responses.size());
        assertSame(firstResponse, responses.get(0));
        assertSame(secondResponse, responses.get(1));
    }

    @Test
    void updateClass_updatesFieldsAndMajor() {
        ClassRequest request = request();
        request.setClassCode("CNTT02");
        request.setNameClass("Information Technology 2");
        ClassEntity existing = ClassEntity.builder().classId(10L).build();
        MajorEntity major = MajorEntity.builder().majorId(1L).build();
        ClassEntity mapped = ClassEntity.builder()
                .classCode("CNTT02")
                .className("Information Technology 2")
                .description("First class")
                .build();
        ClassResponse expected = ClassResponse.builder()
                .idClass(10L)
                .classCode("CNTT02")
                .className("Information Technology 2")
                .build();

        when(classRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(majorRepository.findById(1L)).thenReturn(Optional.of(major));
        when(classMapper.toClassEntity(request)).thenReturn(mapped);
        when(classRepository.save(existing)).thenReturn(existing);
        when(classMapper.toClassResponse(existing)).thenReturn(expected);

        ClassResponse actual = classService.updateClass(10L, request);

        assertSame(expected, actual);
        assertEquals("CNTT02", existing.getClassCode());
        assertEquals("Information Technology 2", existing.getClassName());
        assertSame(major, existing.getMajor());
        verify(classRepository).save(existing);
    }

    @Test
    void updateClass_rejectsDuplicateNameOwnedByAnotherClass() {
        ClassRequest request = request();
        ClassEntity existing = ClassEntity.builder().classId(10L).build();
        when(classRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(classRepository.existsByClassNameIgnoreCaseAndClassIdNot("Information Technology 1", 10L))
                .thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> classService.updateClass(10L, request));

        assertEquals(ErrorCode.INVALID_KEY, exception.getErrorCode());
        verify(classRepository, never()).save(any());
    }

    @Test
    void deleteClass_deletesExistingClass() {
        ClassEntity classEntity = ClassEntity.builder().classId(10L).build();
        when(classRepository.findById(10L)).thenReturn(Optional.of(classEntity));

        classService.deleteClass(10L);

        verify(classRepository).delete(classEntity);
    }

    private ClassRequest request() {
        return ClassRequest.builder()
                .classCode("CNTT01")
                .nameClass("Information Technology 1")
                .majorId("1")
                .description("First class")
                .build();
    }

    private ClassResponse response() {
        return ClassResponse.builder()
                .idClass(10L)
                .classCode("CNTT01")
                .className("Information Technology 1")
                .description("First class")
                .build();
    }
}

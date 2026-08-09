package com.graduration.Service.DerpatmentService;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.DTO.Request.ClassRequest;
import com.graduration.DTO.Response.ClassResponse;
import com.graduration.Repository.ClassRepository;
import com.graduration.Repository.MajorRepository;
import com.graduration.entity.ClassEntity;
import com.graduration.entity.MajorEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.ClassMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClassService {
    ClassRepository classRepository;
    MajorRepository majorRepository;
    ClassMapper classMapper;

    @Transactional
    public ClassResponse createClass(ClassRequest request) {
        normalizeRequest(request);

        if (classRepository.existsByClassCodeIgnoreCase(request.getClassCode())
                || classRepository.existsByClassNameIgnoreCase(request.getNameClass())) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        MajorEntity major = majorRepository
                .findById(parseMajorId(request.getMajorId()))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
        ClassEntity classEntity = classMapper.toClassEntity(request);
        classEntity.setMajor(major);

        return classMapper.toClassResponse(classRepository.save(classEntity));
    }

    @Transactional(readOnly = true)
    public ClassResponse getClass(String classCode) {
        if (classCode == null || classCode.isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        return classMapper.toClassResponse(classRepository
                .findByClassCodeIgnoreCase(classCode.trim())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY)));
    }

    @Transactional(readOnly = true)
    public List<ClassResponse> getAllClasses() {
        return classRepository.findAll().stream()
                .map(classMapper::toClassResponse)
                .toList();
    }

    @Transactional
    public ClassResponse updateClass(Long classId, ClassRequest request) {
        ClassEntity existingClass =
                classRepository.findById(classId).orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
        normalizeRequest(request);

        if (classRepository.existsByClassCodeIgnoreCaseAndClassIdNot(request.getClassCode(), classId)
                || classRepository.existsByClassNameIgnoreCaseAndClassIdNot(request.getNameClass(), classId)) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        MajorEntity major = majorRepository
                .findById(parseMajorId(request.getMajorId()))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
        ClassEntity mappedClass = classMapper.toClassEntity(request);
        existingClass.setClassCode(mappedClass.getClassCode());
        existingClass.setClassName(mappedClass.getClassName());
        existingClass.setDescription(mappedClass.getDescription());
        existingClass.setMajor(major);

        return classMapper.toClassResponse(classRepository.save(existingClass));
    }

    @Transactional
    public void deleteClass(Long classId) {
        ClassEntity classEntity =
                classRepository.findById(classId).orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
        classRepository.delete(classEntity);
    }

    private void normalizeRequest(ClassRequest request) {
        if (request == null
                || request.getClassCode() == null
                || request.getClassCode().isBlank()
                || request.getNameClass() == null
                || request.getNameClass().isBlank()
                || request.getMajorId() == null
                || request.getMajorId().isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        request.setClassCode(request.getClassCode().trim());
        request.setNameClass(request.getNameClass().trim());
        request.setMajorId(request.getMajorId().trim());
        if (request.getDescription() != null) {
            String description = request.getDescription().trim();
            request.setDescription(description.isEmpty() ? null : description);
        }
    }

    private Long parseMajorId(String majorId) {
        try {
            return Long.valueOf(majorId);
        } catch (NumberFormatException exception) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }
}

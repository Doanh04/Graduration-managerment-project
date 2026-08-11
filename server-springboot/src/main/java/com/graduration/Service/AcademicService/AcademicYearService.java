package com.graduration.Service.AcademicService;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.DTO.Request.AcademicYearRequest;
import com.graduration.DTO.Response.AcademicYearResponse;
import com.graduration.Repository.AcademicYearRepository;
import com.graduration.entity.AcademicYearEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.AcademicYearMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AcademicYearService {
    AcademicYearRepository academicYearRepository;
    AcademicYearMapper academicYearMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public AcademicYearResponse createAcademicYear(AcademicYearRequest request) {
        validateAndNormalize(request);
        if (academicYearRepository.existsByAcademicYearIgnoreCase(request.getAcademicYear())) {
            throw new AppException(ErrorCode.ACADEMIC_YEAR_ALREADY_EXISTS);
        }

        AcademicYearEntity academicYear = academicYearMapper.toAcademicYearEntity(request);
        return academicYearMapper.toAcademicYearResponse(academicYearRepository.save(academicYear));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional(readOnly = true)
    public AcademicYearResponse getAcademicYear(Integer academicId) {
        return academicYearMapper.toAcademicYearResponse(findAcademicYear(academicId));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional(readOnly = true)
    public AcademicYearResponse getAcademicYearByName(String academicYear) {
        String normalized = normalizeAcademicYear(academicYear);
        return academicYearMapper.toAcademicYearResponse(academicYearRepository
                .findByAcademicYearIgnoreCase(normalized)
                .orElseThrow(() -> new AppException(ErrorCode.ACADEMIC_YEAR_NOT_FOUND)));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional(readOnly = true)
    public List<AcademicYearResponse> getAllAcademicYears() {
        return getAllAcademicYears(0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional(readOnly = true)
    public List<AcademicYearResponse> getAllAcademicYears(Integer page, Integer size) {
        return academicYearRepository.findAll(PaginationSupport.pageRequest(page, size)).stream()
                .map(academicYearMapper::toAcademicYearResponse)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public AcademicYearResponse updateAcademicYear(Integer academicId, AcademicYearRequest request) {
        AcademicYearEntity academicYear = findAcademicYear(academicId);
        validateAndNormalize(request);
        if (academicYearRepository.existsByAcademicYearIgnoreCaseAndAcademicIdNot(
                request.getAcademicYear(), academicId)) {
            throw new AppException(ErrorCode.ACADEMIC_YEAR_ALREADY_EXISTS);
        }

        academicYearMapper.updateAcademicYear(request, academicYear);
        return academicYearMapper.toAcademicYearResponse(academicYearRepository.save(academicYear));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public void deleteAcademicYear(Integer academicId) {
        AcademicYearEntity academicYear = findAcademicYear(academicId);
        if (!academicYear.getDefensePeriod().isEmpty()
                || !academicYear.getDefenseCommittees().isEmpty()) {
            throw new AppException(ErrorCode.ACADEMIC_YEAR_IN_USE);
        }
        academicYearRepository.delete(academicYear);
    }

    private AcademicYearEntity findAcademicYear(Integer academicId) {
        if (academicId == null) {
            throw new AppException(ErrorCode.ACADEMIC_YEAR_NOT_FOUND);
        }
        return academicYearRepository
                .findById(academicId)
                .orElseThrow(() -> new AppException(ErrorCode.ACADEMIC_YEAR_NOT_FOUND));
    }

    private void validateAndNormalize(AcademicYearRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.ACADEMIC_YEAR_INVALID);
        }
        request.setAcademicYear(normalizeAcademicYear(request.getAcademicYear()));
        request.setDescription(normalize(request.getDescription()));
    }

    private String normalizeAcademicYear(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorCode.ACADEMIC_YEAR_NOT_BLANK);
        }
        String normalized = value.trim().replaceAll("\\s*[/\\-]\\s*", "-");
        if (!normalized.matches("\\d{4}-\\d{4}")) {
            throw new AppException(ErrorCode.ACADEMIC_YEAR_INVALID);
        }
        int startYear = Integer.parseInt(normalized.substring(0, 4));
        int endYear = Integer.parseInt(normalized.substring(5));
        if (endYear <= startYear) {
            throw new AppException(ErrorCode.ACADEMIC_YEAR_INVALID);
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

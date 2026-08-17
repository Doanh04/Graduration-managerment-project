package com.graduration.Service.DerpatmentService;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.DTO.Request.MajorRequest;
import com.graduration.DTO.Response.MajorResponse;
import com.graduration.Repository.MajorRepository;
import com.graduration.entity.MajorEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.MajorMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MajorService {
    MajorRepository majorRepository;
    MajorMapper majorMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public MajorResponse createMajor(MajorRequest request) {
        validateAndNormalizeRequest(request);
        if (majorRepository.existsByMajorNameIgnoreCase(request.getMajorName())) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        MajorEntity major = majorMapper.toMajorEntity(request);
        return majorMapper.toMajorResponse(majorRepository.save(major));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    public MajorResponse getMajor(Long majorId) {
        return majorMapper.toMajorResponse(findMajor(majorId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    public List<MajorResponse> getAllMajors() {
        return getAllMajors(0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    public List<MajorResponse> getAllMajors(Integer page, Integer size) {
        return majorRepository.findAll(PaginationSupport.pageRequest(page, size)).stream()
                .map(majorMapper::toMajorResponse)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    public com.graduration.DTO.Response.PageResponse<MajorResponse> getAllMajorsPage(Integer page, Integer size) {
        return com.graduration.DTO.Response.PageResponse.from(
                majorRepository.findAll(PaginationSupport.pageRequest(page, size)), majorMapper::toMajorResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public MajorResponse updateMajor(Long majorId, MajorRequest request) {
        MajorEntity major = findMajor(majorId);
        validateAndNormalizeRequest(request);
        if (majorRepository.existsByMajorNameIgnoreCaseAndMajorIdNot(request.getMajorName(), majorId)) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        MajorEntity mappedMajor = majorMapper.toMajorEntity(request);
        major.setMajorName(mappedMajor.getMajorName());
        major.setDescription(mappedMajor.getDescription());

        return majorMapper.toMajorResponse(majorRepository.save(major));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public void deleteMajor(Long majorId) {
        MajorEntity major = findMajor(majorId);
        if (!major.getClassEntity().isEmpty()) {
            throw new AppException(ErrorCode.MAJOR_IN_USE);
        }
        majorRepository.delete(major);
    }

    private MajorEntity findMajor(Long majorId) {
        if (majorId == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        return majorRepository.findById(majorId).orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
    }

    private void validateAndNormalizeRequest(MajorRequest request) {
        if (request == null
                || request.getMajorName() == null
                || request.getMajorName().isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        request.setMajorName(request.getMajorName().trim());
        if (request.getDescription() != null) {
            String description = request.getDescription().trim();
            request.setDescription(description.isEmpty() ? null : description);
        }
    }
}

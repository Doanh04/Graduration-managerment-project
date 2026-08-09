package com.graduration.Service.DerpatmentService;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public MajorResponse createMajor(MajorRequest request) {
        validateAndNormalizeRequest(request);
        if (majorRepository.existsByMajorNameIgnoreCase(request.getMajorName())) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        MajorEntity major = majorMapper.toMajorEntity(request);
        return majorMapper.toMajorResponse(majorRepository.save(major));
    }

    @Transactional(readOnly = true)
    public MajorResponse getMajor(Long majorId) {
        return majorMapper.toMajorResponse(findMajor(majorId));
    }

    @Transactional(readOnly = true)
    public List<MajorResponse> getAllMajors() {
        return majorRepository.findAll().stream()
                .map(majorMapper::toMajorResponse)
                .toList();
    }

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

    @Transactional
    public void deleteMajor(Long majorId) {
        majorRepository.delete(findMajor(majorId));
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

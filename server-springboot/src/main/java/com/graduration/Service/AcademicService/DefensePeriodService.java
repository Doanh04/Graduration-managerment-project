package com.graduration.Service.AcademicService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.DTO.Request.DefensePeriodRequest;
import com.graduration.DTO.Response.DefensePeriodResponse;
import com.graduration.Repository.AcademicYearRepository;
import com.graduration.Repository.DefensePeriodRepository;
import com.graduration.entity.AcademicYearEntity;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.DefensePeriodMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefensePeriodService {
    DefensePeriodRepository defensePeriodRepository;
    AcademicYearRepository academicYearRepository;
    DefensePeriodMapper defensePeriodMapper;
    Clock clock = Clock.systemDefaultZone();

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public DefensePeriodResponse createDefensePeriod(DefensePeriodRequest request) {
        validateAndNormalize(request);
        AcademicYearEntity academicYear = findAcademicYear(request.getAcademicId());
        validateUniqueName(request, null);

        DefensePeriodEntity defensePeriod = defensePeriodMapper.toDefensePeriodEntity(request);
        defensePeriod.setAcademicYear(academicYear);
        applyFinishedStatus(defensePeriod);
        return defensePeriodMapper.toDefensePeriodResponse(defensePeriodRepository.save(defensePeriod));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional
    public DefensePeriodResponse getDefensePeriod(Long defensePeriodId) {
        DefensePeriodEntity defensePeriod = findDefensePeriod(defensePeriodId);
        updateFinishedStatusIfNecessary(defensePeriod);
        return defensePeriodMapper.toDefensePeriodResponse(defensePeriod);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional
    public List<DefensePeriodResponse> getAllDefensePeriods() {
        return getAllDefensePeriods(0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional
    public List<DefensePeriodResponse> getAllDefensePeriods(Integer page, Integer size) {
        finishExpiredPeriods();
        return defensePeriodRepository.findAllByOrderByStartDateDesc(PaginationSupport.pageRequest(page, size)).stream()
                .map(defensePeriodMapper::toDefensePeriodResponse)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional
    public List<DefensePeriodResponse> getDefensePeriodsByAcademicYear(Integer academicId) {
        return getDefensePeriodsByAcademicYear(academicId, 0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional
    public List<DefensePeriodResponse> getDefensePeriodsByAcademicYear(Integer academicId, Integer page, Integer size) {
        findAcademicYear(academicId);
        finishExpiredPeriods();
        return defensePeriodRepository
                .findAllByAcademicYear_AcademicIdOrderByStartDateDesc(
                        academicId, PaginationSupport.pageRequest(page, size))
                .stream()
                .map(defensePeriodMapper::toDefensePeriodResponse)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public DefensePeriodResponse updateDefensePeriod(Long defensePeriodId, DefensePeriodRequest request) {
        DefensePeriodEntity defensePeriod = findDefensePeriod(defensePeriodId);
        validateAndNormalize(request);
        AcademicYearEntity academicYear = findAcademicYear(request.getAcademicId());
        validateUniqueName(request, defensePeriodId);

        defensePeriodMapper.updateDefensePeriod(request, defensePeriod);
        defensePeriod.setAcademicYear(academicYear);
        applyFinishedStatus(defensePeriod);
        return defensePeriodMapper.toDefensePeriodResponse(defensePeriodRepository.save(defensePeriod));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public void deleteDefensePeriod(Long defensePeriodId) {
        DefensePeriodEntity defensePeriod = findDefensePeriod(defensePeriodId);
        if (!defensePeriod.getTopic().isEmpty()
                || !defensePeriod.getMilesStone().isEmpty()) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_IN_USE);
        }
        defensePeriodRepository.delete(defensePeriod);
    }

    @Scheduled(fixedDelayString = "${academic.defense-period.status-check-ms:60000}")
    @Transactional
    public int finishExpiredPeriods() {
        return defensePeriodRepository.markExpiredPeriodsFinished(LocalDate.now(clock), DefensePeriodConstain.FINISHED);
    }

    private void updateFinishedStatusIfNecessary(DefensePeriodEntity defensePeriod) {
        if (applyFinishedStatus(defensePeriod)) {
            defensePeriodRepository.save(defensePeriod);
        }
    }

    private boolean applyFinishedStatus(DefensePeriodEntity defensePeriod) {
        if (defensePeriod.getEndDate().isBefore(LocalDate.now(clock))
                && defensePeriod.getStatus() != DefensePeriodConstain.FINISHED) {
            defensePeriod.setStatus(DefensePeriodConstain.FINISHED);
            return true;
        }
        return false;
    }

    private DefensePeriodEntity findDefensePeriod(Long defensePeriodId) {
        if (defensePeriodId == null) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND);
        }
        return defensePeriodRepository
                .findById(defensePeriodId)
                .orElseThrow(() -> new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND));
    }

    private AcademicYearEntity findAcademicYear(Integer academicId) {
        if (academicId == null) {
            throw new AppException(ErrorCode.ACADEMIC_YEAR_NOT_FOUND);
        }
        return academicYearRepository
                .findById(academicId)
                .orElseThrow(() -> new AppException(ErrorCode.ACADEMIC_YEAR_NOT_FOUND));
    }

    private void validateUniqueName(DefensePeriodRequest request, Long defensePeriodId) {
        boolean exists = defensePeriodId == null
                ? defensePeriodRepository.existsByPeriodNameIgnoreCaseAndAcademicYear_AcademicId(
                        request.getPeriodName(), request.getAcademicId())
                : defensePeriodRepository.existsDuplicatePeriodName(
                        request.getPeriodName(), request.getAcademicId(), defensePeriodId);
        if (exists) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_ALREADY_EXISTS);
        }
    }

    private void validateAndNormalize(DefensePeriodRequest request) {
        if (request == null
                || request.getPeriodName() == null
                || request.getPeriodName().isBlank()
                || request.getStartDate() == null
                || request.getEndDate() == null
                || request.getStatus() == null) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_INVALID);
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_INVALID_DATE);
        }
        request.setPeriodName(request.getPeriodName().trim());
        request.setProjectType(normalize(request.getProjectType()));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

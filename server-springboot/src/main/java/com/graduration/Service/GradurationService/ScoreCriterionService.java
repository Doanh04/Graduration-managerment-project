package com.graduration.Service.GradurationService;

import java.math.BigDecimal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.Constain.ScoreTypeConstain;
import com.graduration.DTO.Request.ScoreCriterionRequest;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.ScoreCriterionResponse;
import com.graduration.Repository.DefensePeriodRepository;
import com.graduration.Repository.ScoreCriterionRepository;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.ScoreCriterionEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.ScoreMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScoreCriterionService {
    ScoreCriterionRepository criterionRepository;
    DefensePeriodRepository defensePeriodRepository;
    ScoreMapper scoreMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public ScoreCriterionResponse create(Long defensePeriodId, ScoreCriterionRequest request) {
        DefensePeriodEntity period = findActivePeriod(defensePeriodId);
        normalizeAndValidate(request);
        if (criterionRepository.existsDuplicateCode(request.getCriterionCode(), defensePeriodId, null)) {
            throw new AppException(ErrorCode.SCORE_CRITERION_ALREADY_EXISTS);
        }
        ScoreCriterionEntity criterion = ScoreCriterionEntity.builder()
                .criterionCode(request.getCriterionCode())
                .criterionName(request.getCriterionName())
                .description(normalize(request.getDescription()))
                .scoreType(ScoreTypeConstain.FINAL)
                .maxScore(request.getMaxScore())
                .weight(request.getWeight())
                .displayOrder(request.getDisplayOrder())
                .active(request.getActive() == null || request.getActive())
                .defensePeriod(period)
                .build();
        return scoreMapper.toCriterionResponse(criterionRepository.save(criterion));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public PageResponse<ScoreCriterionResponse> getByDefensePeriod(Long defensePeriodId, Integer page, Integer size) {
        if (!defensePeriodRepository.existsById(defensePeriodId)) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND);
        }
        return PageResponse.from(
                criterionRepository.findByDefensePeriod(defensePeriodId, PaginationSupport.pageRequest(page, size)),
                scoreMapper::toCriterionResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public ScoreCriterionResponse update(Long criterionId, ScoreCriterionRequest request) {
        ScoreCriterionEntity criterion = findCriterion(criterionId);
        if (!criterion.getScoreDetails().isEmpty()) {
            throw new AppException(ErrorCode.SCORE_CRITERION_IN_USE);
        }
        findActivePeriod(criterion.getDefensePeriod().getID_Defense());
        normalizeAndValidate(request);
        if (criterionRepository.existsDuplicateCode(
                request.getCriterionCode(), criterion.getDefensePeriod().getID_Defense(), criterionId)) {
            throw new AppException(ErrorCode.SCORE_CRITERION_ALREADY_EXISTS);
        }
        criterion.setCriterionCode(request.getCriterionCode());
        criterion.setCriterionName(request.getCriterionName());
        criterion.setDescription(normalize(request.getDescription()));
        criterion.setMaxScore(request.getMaxScore());
        criterion.setWeight(request.getWeight());
        criterion.setDisplayOrder(request.getDisplayOrder());
        criterion.setActive(request.getActive() == null || request.getActive());
        criterion.setScoreType(ScoreTypeConstain.FINAL);
        return scoreMapper.toCriterionResponse(criterionRepository.save(criterion));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public void delete(Long criterionId) {
        ScoreCriterionEntity criterion = findCriterion(criterionId);
        if (!criterion.getScoreDetails().isEmpty()) {
            throw new AppException(ErrorCode.SCORE_CRITERION_IN_USE);
        }
        criterionRepository.delete(criterion);
    }

    private ScoreCriterionEntity findCriterion(Long criterionId) {
        return criterionRepository
                .findById(criterionId)
                .orElseThrow(() -> new AppException(ErrorCode.SCORE_CRITERION_NOT_FOUND));
    }

    private DefensePeriodEntity findActivePeriod(Long defensePeriodId) {
        DefensePeriodEntity period = defensePeriodRepository
                .findById(defensePeriodId)
                .orElseThrow(() -> new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND));
        if (period.getStatus() == DefensePeriodConstain.FINISHED) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_FINISHED);
        }
        return period;
    }

    private void normalizeAndValidate(ScoreCriterionRequest request) {
        if (request == null
                || request.getCriterionCode() == null
                || request.getCriterionCode().isBlank()) {
            throw new AppException(ErrorCode.SCORE_CRITERION_CODE_NOT_BLANK);
        }
        if (request.getCriterionName() == null || request.getCriterionName().isBlank()) {
            throw new AppException(ErrorCode.SCORE_CRITERION_NAME_NOT_BLANK);
        }
        if (request.getMaxScore() == null
                || request.getMaxScore().compareTo(BigDecimal.ZERO) <= 0
                || request.getMaxScore().compareTo(BigDecimal.TEN) > 0) {
            throw new AppException(ErrorCode.SCORE_CRITERION_MAX_SCORE_INVALID);
        }
        if (request.getWeight() == null
                || request.getWeight().compareTo(BigDecimal.ZERO) <= 0
                || request.getWeight().compareTo(new BigDecimal("100.00")) > 0) {
            throw new AppException(ErrorCode.SCORE_CRITERION_WEIGHT_INVALID);
        }
        request.setCriterionCode(request.getCriterionCode().trim().toUpperCase(java.util.Locale.ROOT));
        request.setCriterionName(request.getCriterionName().trim());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

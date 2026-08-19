package com.graduration.Service.GradurationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.ScoreStatusConstain;
import com.graduration.Constain.ScoreTypeConstain;
import com.graduration.Constain.SupervisorAssignmentStatusConstain;
import com.graduration.DTO.Request.ScoreRequest;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.ScoreResponse;
import com.graduration.Repository.ScoreCriterionRepository;
import com.graduration.Repository.ScoreRepository;
import com.graduration.Repository.StudentRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.ScoreCriterionEntity;
import com.graduration.entity.ScoreDetailEntity;
import com.graduration.entity.ScoreEntity;
import com.graduration.entity.StudentEntity;
import com.graduration.entity.TopicEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.ScoreMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScoreService {
    private static final BigDecimal TOTAL_WEIGHT = new BigDecimal("100.00");
    private static final BigDecimal SCORE_SCALE = new BigDecimal("10.00");

    ScoreRepository scoreRepository;
    ScoreCriterionRepository criterionRepository;
    StudentRepository studentRepository;
    TopicRepository topicRepository;
    UserRepository userRepository;
    ScoreMapper scoreMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional
    public ScoreResponse saveDraft(String studentId, Long topicId, ScoreRequest request) {
        StudentEntity student =
                studentRepository.findById(studentId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        TopicEntity topic =
                topicRepository.findById(topicId).orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
        requireStudentTopic(student, topic);
        requireScoringAccess(topic);

        ScoreEntity score = scoreRepository
                .findStudentTopicScore(studentId, topicId)
                .orElseGet(() -> ScoreEntity.builder()
                        .student(student)
                        .team(topic.getTeam())
                        .topic(topic)
                        .createdBy(currentUser())
                        .scoreType(ScoreTypeConstain.FINAL)
                        .status(ScoreStatusConstain.DRAFT)
                        .build());
        if (score.getStatus() != ScoreStatusConstain.DRAFT) {
            throw new AppException(ErrorCode.SCORE_OPERATION_NOT_ALLOWED);
        }
        applyDetails(score, request, topic.getDefensePeriod().getID_Defense());
        score.setComment(normalize(request.getComment()));
        return scoreMapper.toScoreResponse(scoreRepository.save(score));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ScoreResponse getScore(Long scoreId) {
        ScoreEntity score = findScore(scoreId);
        requireReadAccess(score);
        return scoreMapper.toScoreResponse(score);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional
    public ScoreResponse submit(Long scoreId) {
        ScoreEntity score = findScore(scoreId);
        requireScoringAccess(score.getTopic());
        requireStatus(score, ScoreStatusConstain.DRAFT);
        score.setStatus(ScoreStatusConstain.SUBMITTED);
        score.setSubmittedAt(LocalDateTime.now());
        return scoreMapper.toScoreResponse(scoreRepository.save(score));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public ScoreResponse publish(Long scoreId) {
        ScoreEntity score = findScore(scoreId);
        requireStatus(score, ScoreStatusConstain.SUBMITTED);
        score.setStatus(ScoreStatusConstain.LOCKED);
        score.setPublishedAt(LocalDateTime.now());
        return scoreMapper.toScoreResponse(scoreRepository.save(score));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    public ScoreResponse unlock(Long scoreId) {
        ScoreEntity score = findScore(scoreId);
        requireStatus(score, ScoreStatusConstain.LOCKED);
        score.setStatus(ScoreStatusConstain.DRAFT);
        score.setSubmittedAt(null);
        score.setPublishedAt(null);
        return scoreMapper.toScoreResponse(scoreRepository.save(score));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    public PageResponse<ScoreResponse> getByDefensePeriod(Long defensePeriodId, Integer page, Integer size) {
        return PageResponse.from(
                scoreRepository.findByDefensePeriod(defensePeriodId, PaginationSupport.pageRequest(page, size)),
                scoreMapper::toScoreResponse);
    }

    private void applyDetails(ScoreEntity score, ScoreRequest request, Long defensePeriodId) {
        if (request == null
                || request.getDetails() == null
                || request.getDetails().isEmpty()) {
            throw new AppException(ErrorCode.SCORE_DETAILS_NOT_EMPTY);
        }
        List<ScoreCriterionEntity> criteria = criterionRepository.findActiveByDefensePeriod(defensePeriodId);
        BigDecimal totalWeight =
                criteria.stream().map(ScoreCriterionEntity::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.compareTo(TOTAL_WEIGHT) != 0) {
            throw new AppException(ErrorCode.SCORE_CRITERIA_WEIGHT_INVALID);
        }
        Map<Long, ScoreRequest.Detail> requested = new HashMap<>();
        for (ScoreRequest.Detail detail : request.getDetails()) {
            if (detail == null
                    || detail.getCriterionId() == null
                    || requested.put(detail.getCriterionId(), detail) != null) {
                throw new AppException(ErrorCode.SCORE_CRITERIA_MISMATCH);
            }
        }
        if (requested.size() != criteria.size()
                || criteria.stream().anyMatch(criterion -> !requested.containsKey(criterion.getCriterionId()))) {
            throw new AppException(ErrorCode.SCORE_CRITERIA_MISMATCH);
        }

        score.getDetails().clear();
        BigDecimal total = BigDecimal.ZERO;
        for (ScoreCriterionEntity criterion : criteria) {
            ScoreRequest.Detail input = requested.get(criterion.getCriterionId());
            if (input.getScore() == null
                    || input.getScore().compareTo(BigDecimal.ZERO) < 0
                    || input.getScore().compareTo(criterion.getMaxScore()) > 0) {
                throw new AppException(ErrorCode.SCORE_VALUE_INVALID);
            }
            BigDecimal weighted = input.getScore()
                    .divide(criterion.getMaxScore(), 8, RoundingMode.HALF_UP)
                    .multiply(criterion.getWeight())
                    .divide(SCORE_SCALE, 2, RoundingMode.HALF_UP);
            score.getDetails()
                    .add(ScoreDetailEntity.builder()
                            .score(score)
                            .criterion(criterion)
                            .scoreValue(input.getScore().setScale(2, RoundingMode.HALF_UP))
                            .weightedScore(weighted)
                            .comment(normalize(input.getComment()))
                            .build());
            total = total.add(weighted);
        }
        score.setScore(total.setScale(2, RoundingMode.HALF_UP));
    }

    private void requireStudentTopic(StudentEntity student, TopicEntity topic) {
        if (topic.getTeam() == null
                || student.getTeam() == null
                || !topic.getTeam().getIdTeam().equals(student.getTeam().getIdTeam())) {
            throw new AppException(ErrorCode.SCORE_STUDENT_TOPIC_MISMATCH);
        }
    }

    private void requireScoringAccess(TopicEntity topic) {
        if (isManager()) {
            return;
        }
        String userId = currentAuthentication().getName();
        boolean supervisor = topic.getTopicSuperVisorEntities().stream()
                .anyMatch(item -> item.getLecture() != null
                        && item.getStatus() == SupervisorAssignmentStatusConstain.ACTIVE
                        && item.getLecture().getUser() != null
                        && userId.equals(item.getLecture().getUser().getUserId()));
        if (!supervisor) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void requireReadAccess(ScoreEntity score) {
        if (isManager()) {
            return;
        }
        String userId = currentAuthentication().getName();
        boolean ownPublishedScore = score.getStudent().getUserEntity() != null
                && userId.equals(score.getStudent().getUserEntity().getUserId())
                && score.getStatus() == ScoreStatusConstain.LOCKED;
        if (ownPublishedScore) {
            return;
        }
        requireScoringAccess(score.getTopic());
    }

    private ScoreEntity findScore(Long scoreId) {
        return scoreRepository
                .findWithDetailsById(scoreId)
                .orElseThrow(() -> new AppException(ErrorCode.SCORE_NOT_FOUND));
    }

    private void requireStatus(ScoreEntity score, ScoreStatusConstain expected) {
        if (score.getStatus() != expected) {
            throw new AppException(ErrorCode.SCORE_OPERATION_NOT_ALLOWED);
        }
    }

    private boolean isManager() {
        return hasAuthority("ROLE_ADMIN") || hasAuthority("ROLE_FACULTY");
    }

    private boolean hasAuthority(String authority) {
        return currentAuthentication().getAuthorities().stream()
                .anyMatch(item -> item.getAuthority().equals(authority));
    }

    private UserEntity currentUser() {
        return userRepository
                .findById(currentAuthentication().getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Authentication currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

package com.graduration.Service.GradurationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
import com.graduration.Repository.LectureRepository;
import com.graduration.Repository.ScoreRepository;
import com.graduration.Repository.StudentRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.entity.LectureEntity;
import com.graduration.entity.ScoreEntity;
import com.graduration.entity.StudentEntity;
import com.graduration.entity.TopicEntity;
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
    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = new BigDecimal("10.00");

    ScoreRepository scoreRepository;
    LectureRepository lectureRepository;
    StudentRepository studentRepository;
    TopicRepository topicRepository;
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
        LectureEntity evaluator = currentLecturer();

        ScoreEntity score = scoreRepository
                .findStudentTopicScore(studentId, topicId, evaluator.getLectureId(), ScoreTypeConstain.FINAL)
                .orElseGet(() -> ScoreEntity.builder()
                        .student(student)
                        .topic(topic)
                        .lecture(evaluator)
                        .scoreType(ScoreTypeConstain.FINAL)
                        .status(ScoreStatusConstain.DRAFT)
                        .build());
        if (score.getLecture() == null) {
            score.setLecture(evaluator);
        }
        if (score.getStatus() != ScoreStatusConstain.DRAFT) {
            throw new AppException(ErrorCode.SCORE_OPERATION_NOT_ALLOWED);
        }
        applyScore(score, request);
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

    private void applyScore(ScoreEntity score, ScoreRequest request) {
        if (request == null
                || request.getScore() == null
                || request.getScore().compareTo(MIN_SCORE) < 0
                || request.getScore().compareTo(MAX_SCORE) > 0) {
            throw new AppException(ErrorCode.SCORE_VALUE_INVALID);
        }
        score.setScore(request.getScore().setScale(2, java.math.RoundingMode.HALF_UP));
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
                .findWithRelationsById(scoreId)
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

    private LectureEntity currentLecturer() {
        return lectureRepository
                .findByUser_UserId(currentAuthentication().getName())
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

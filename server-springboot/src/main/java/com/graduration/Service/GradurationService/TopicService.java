package com.graduration.Service.GradurationService;

import java.util.Locale;
import java.util.Objects;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.CategoryTopicConstain;
import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.Constain.TopicStatusConstain;
import com.graduration.DTO.Request.CreateTopicRequest;
import com.graduration.DTO.Request.UpdateTopicRequest;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.TopicResponse;
import com.graduration.Repository.DefensePeriodRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.TopicEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.TopicMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TopicService {
    TopicRepository topicRepository;
    DefensePeriodRepository defensePeriodRepository;
    TopicMapper topicMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_STUDENT')")
    @Transactional
    public TopicResponse createTopic(CreateTopicRequest request) {
        validateRequest(
                request == null ? null : request.getTitle(), request == null ? null : request.getCategoryTopic());
        DefensePeriodEntity period = findActiveDefensePeriod(request.getDefensePeriodId());
        String title = request.getTitle().trim();
        if (topicRepository.existsTitleInDefensePeriod(title, period.getID_Defense())) {
            throw new AppException(ErrorCode.TOPIC_ALREADY_EXISTS);
        }
        request.setTitle(title);
        normalize(request);
        TopicEntity topic = topicMapper.toEntity(request);
        topic.setDefensePeriod(period);
        topic.setCreatedBy(currentUserId());
        topic.setStatus(TopicStatusConstain.DRAFT);
        return topicMapper.toResponse(topicRepository.save(topic));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public TopicResponse getTopic(Long topicId) {
        return topicMapper.toResponse(findTopic(topicId));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public PageResponse<TopicResponse> getTopics(
            Integer page,
            Integer size,
            Integer academicYearId,
            Long defensePeriodId,
            CategoryTopicConstain categoryTopic,
            TopicStatusConstain status,
            String keyword) {
        Specification<TopicEntity> specification = Specification.where(null);
        if (academicYearId != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("defensePeriod").get("academicYear").get("academicId"), academicYearId));
        }
        if (defensePeriodId != null) {
            specification = specification.and(
                    (root, query, cb) -> cb.equal(root.get("defensePeriod").get("ID_Defense"), defensePeriodId));
        }
        if (categoryTopic != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("categoryTopic"), categoryTopic));
        }
        if (status != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(root.get("technology")), pattern)));
        }
        return PageResponse.from(
                topicRepository.findAll(
                        specification,
                        PaginationSupport.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))),
                topicMapper::toResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_STUDENT')")
    @Transactional
    public TopicResponse updateTopic(Long topicId, UpdateTopicRequest request) {
        TopicEntity topic = findTopic(topicId);
        requireOwnerOrManager(topic);
        requireEditable(topic);
        validateRequest(
                request == null ? null : request.getTitle(), request == null ? null : request.getCategoryTopic());
        DefensePeriodEntity period = findActiveDefensePeriod(request.getDefensePeriodId());
        String title = request.getTitle().trim();
        if (topicRepository.existsDuplicateTitle(title, period.getID_Defense(), topicId)) {
            throw new AppException(ErrorCode.TOPIC_ALREADY_EXISTS);
        }
        request.setTitle(title);
        normalize(request);
        topicMapper.update(request, topic);
        topic.setDefensePeriod(period);
        return topicMapper.toResponse(topicRepository.save(topic));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_STUDENT')")
    @Transactional
    public void deleteTopic(Long topicId) {
        TopicEntity topic = findTopic(topicId);
        requireOwnerOrManager(topic);
        requireEditable(topic);
        if (topic.getTeam() != null
                || !topic.getTopicSuperVisorEntities().isEmpty()
                || !topic.getReviewAssignment().isEmpty()
                || topic.getDefenseSchedule() != null) {
            throw new AppException(ErrorCode.TOPIC_IN_USE);
        }
        topicRepository.delete(topic);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_STUDENT')")
    @Transactional
    public TopicResponse submitForApproval(Long topicId) {
        TopicEntity topic = findTopic(topicId);
        requireOwnerOrManager(topic);
        requireEditable(topic);
        requireActiveDefensePeriod(topic.getDefensePeriod());
        topic.setStatus(TopicStatusConstain.PENDING_APPROVAL);
        topic.setRejectionReason(null);
        return topicMapper.toResponse(topicRepository.save(topic));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public TopicResponse approveTopic(Long topicId) {
        TopicEntity topic = findTopic(topicId);
        requireStatus(topic, TopicStatusConstain.PENDING_APPROVAL);
        topic.setStatus(TopicStatusConstain.APPROVED);
        topic.setRejectionReason(null);
        return topicMapper.toResponse(topicRepository.save(topic));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public TopicResponse rejectTopic(Long topicId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new AppException(ErrorCode.TOPIC_REJECTION_REASON_NOT_BLANK);
        }
        TopicEntity topic = findTopic(topicId);
        requireStatus(topic, TopicStatusConstain.PENDING_APPROVAL);
        topic.setStatus(TopicStatusConstain.REJECTED);
        topic.setRejectionReason(reason.trim());
        return topicMapper.toResponse(topicRepository.save(topic));
    }

    private TopicEntity findTopic(Long topicId) {
        if (topicId == null) {
            throw new AppException(ErrorCode.TOPIC_NOT_FOUND);
        }
        return topicRepository.findById(topicId).orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
    }

    private DefensePeriodEntity findActiveDefensePeriod(Long defensePeriodId) {
        if (defensePeriodId == null) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND);
        }
        DefensePeriodEntity period = defensePeriodRepository
                .findById(defensePeriodId)
                .orElseThrow(() -> new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND));
        requireActiveDefensePeriod(period);
        return period;
    }

    private void requireActiveDefensePeriod(DefensePeriodEntity period) {
        if (period.getStatus() == DefensePeriodConstain.FINISHED) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_FINISHED);
        }
    }

    private void validateRequest(String title, CategoryTopicConstain category) {
        if (title == null || title.isBlank()) {
            throw new AppException(ErrorCode.TOPIC_TITLE_NOT_BLANK);
        }
        if (category == null) {
            throw new AppException(ErrorCode.TOPIC_CATEGORY_NOT_BLANK);
        }
    }

    private void requireEditable(TopicEntity topic) {
        if (topic.getStatus() != TopicStatusConstain.DRAFT && topic.getStatus() != TopicStatusConstain.REJECTED) {
            throw new AppException(ErrorCode.TOPIC_OPERATION_NOT_ALLOWED);
        }
    }

    private void requireStatus(TopicEntity topic, TopicStatusConstain expected) {
        if (topic.getStatus() != expected) {
            throw new AppException(ErrorCode.TOPIC_OPERATION_NOT_ALLOWED);
        }
    }

    private void requireOwnerOrManager(TopicEntity topic) {
        Authentication authentication = currentAuthentication();
        boolean manager = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_FACULTY"));
        if (!manager && !Objects.equals(topic.getCreatedBy(), authentication.getName())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    private String currentUserId() {
        return currentAuthentication().getName();
    }

    private Authentication currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication;
    }

    private void normalize(CreateTopicRequest request) {
        request.setDescription(normalize(request.getDescription()));
        request.setObjective(normalize(request.getObjective()));
        request.setTechnology(normalize(request.getTechnology()));
    }

    private void normalize(UpdateTopicRequest request) {
        request.setDescription(normalize(request.getDescription()));
        request.setObjective(normalize(request.getObjective()));
        request.setTechnology(normalize(request.getTechnology()));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

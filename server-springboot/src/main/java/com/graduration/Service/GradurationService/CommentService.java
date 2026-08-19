package com.graduration.Service.GradurationService;

import java.time.LocalDateTime;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.CommentTypeConstain;
import com.graduration.Constain.SupervisorAssignmentStatusConstain;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.SubmissionCommentResponse;
import com.graduration.Repository.CommentRepository;
import com.graduration.Repository.LectureRepository;
import com.graduration.Repository.SubmissionRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.CommentEntity;
import com.graduration.entity.LectureEntity;
import com.graduration.entity.SubmistionEntity;
import com.graduration.entity.TeamEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.SubmissionMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentService {
    CommentRepository commentRepository;
    SubmissionRepository submissionRepository;
    UserRepository userRepository;
    LectureRepository lectureRepository;
    SubmissionMapper submissionMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional
    public SubmissionCommentResponse addComment(Long submissionId, String content) {
        validateContent(content);
        SubmistionEntity submission = findSubmission(submissionId);
        requireReviewAccess(submission);
        return submissionMapper.toCommentResponse(createComment(submission, content, CommentTypeConstain.COMMENT));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public PageResponse<SubmissionCommentResponse> getComments(Long submissionId, Integer page, Integer size) {
        SubmistionEntity submission = findSubmission(submissionId);
        requireReadAccess(submission);
        return PageResponse.from(
                commentRepository.findBySubmistion_IdSubmissionOrderByCreatedAtAsc(
                        submissionId, PaginationSupport.pageRequest(page, size)),
                submissionMapper::toCommentResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional
    public SubmissionCommentResponse updateComment(Long commentId, String content) {
        validateContent(content);
        CommentEntity comment = findComment(commentId);
        requireOwnerOrAdmin(comment);
        requireOrdinaryComment(comment);
        comment.setContent(content.trim());
        comment.setEdited(true);
        return submissionMapper.toCommentResponse(commentRepository.save(comment));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional
    public void deleteComment(Long commentId) {
        CommentEntity comment = findComment(commentId);
        requireOwnerOrAdmin(comment);
        requireOrdinaryComment(comment);
        comment.setDeletedAt(LocalDateTime.now());
        commentRepository.save(comment);
    }

    @Transactional
    public CommentEntity createWorkflowComment(
            SubmistionEntity submission, String content, CommentTypeConstain commentType) {
        validateContent(content);
        requireReviewAccess(submission);
        if (commentType == CommentTypeConstain.COMMENT) {
            throw new AppException(ErrorCode.COMMENT_OPERATION_NOT_ALLOWED);
        }
        return createComment(submission, content, commentType);
    }

    private CommentEntity createComment(SubmistionEntity submission, String content, CommentTypeConstain commentType) {
        UserEntity author = currentUser();
        LectureEntity lecturer =
                lectureRepository.findByUser_UserId(author.getUserId()).orElse(null);
        return commentRepository.save(CommentEntity.builder()
                .content(content.trim())
                .commentType(commentType)
                .submistion(submission)
                .lecture(lecturer)
                .createdBy(author)
                .edited(false)
                .build());
    }

    private void requireOrdinaryComment(CommentEntity comment) {
        if (comment.getCommentType() != CommentTypeConstain.COMMENT) {
            throw new AppException(ErrorCode.COMMENT_OPERATION_NOT_ALLOWED);
        }
    }

    private void requireOwnerOrAdmin(CommentEntity comment) {
        String userId = currentAuthentication().getName();
        if (!hasAuthority("ROLE_ADMIN")
                && (comment.getCreatedBy() == null
                        || !userId.equals(comment.getCreatedBy().getUserId()))) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void requireReadAccess(SubmistionEntity submission) {
        if (isManager() || isTeamMember(submission.getTeam()) || isAssignedSupervisor(submission.getTeam())) {
            return;
        }
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    private void requireReviewAccess(SubmistionEntity submission) {
        if (isManager() || isAssignedSupervisor(submission.getTeam())) {
            return;
        }
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    private boolean isTeamMember(TeamEntity team) {
        String userId = currentAuthentication().getName();
        return team.getStudentEntities().stream()
                .anyMatch(student -> student.getUserEntity() != null
                        && userId.equals(student.getUserEntity().getUserId()));
    }

    private boolean isAssignedSupervisor(TeamEntity team) {
        String userId = currentAuthentication().getName();
        return team.getTopic() != null
                && team.getTopic().getTopicSuperVisorEntities().stream()
                        .anyMatch(supervisor -> supervisor.getLecture() != null
                                && supervisor.getStatus() == SupervisorAssignmentStatusConstain.ACTIVE
                                && supervisor.getLecture().getUser() != null
                                && userId.equals(
                                        supervisor.getLecture().getUser().getUserId()));
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

    private SubmistionEntity findSubmission(Long submissionId) {
        return submissionRepository
                .findById(submissionId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));
    }

    private CommentEntity findComment(Long commentId) {
        return commentRepository
                .findActiveById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new AppException(ErrorCode.SUBMISSION_COMMENT_NOT_BLANK);
        }
    }
}

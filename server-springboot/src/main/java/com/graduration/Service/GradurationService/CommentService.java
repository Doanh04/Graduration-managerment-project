package com.graduration.Service.GradurationService;

import java.time.LocalDateTime;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.CommentTypeConstain;
import com.graduration.Constain.CommitteeMemberStatusConstain;
import com.graduration.Constain.DefenseScheduleStatusConstain;
import com.graduration.Constain.SupervisorAssignmentStatusConstain;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.SubmissionCommentResponse;
import com.graduration.Repository.CommentRepository;
import com.graduration.Repository.DefenseScheduleRepository;
import com.graduration.Repository.LectureRepository;
import com.graduration.Repository.SubmissionRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.CommentEntity;
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
    DefenseScheduleRepository defenseScheduleRepository;
    LectureRepository lectureRepository;
    SubmissionRepository submissionRepository;
    UserRepository userRepository;
    SubmissionMapper submissionMapper;

    @PreAuthorize("isAuthenticated()")
    @Transactional
    // Hàm addComment: Nhận submissionId và nội dung nhận xét; kiểm tra nội dung không rỗng, xác minh quyền review rồi
    // tạo comment loại COMMENT gắn với bài nộp.
    public SubmissionCommentResponse addComment(Long submissionId, String content) {
        validateContent(content);
        SubmistionEntity submission = findSubmission(submissionId);
        requireReviewAccess(submission);
        return submissionMapper.toCommentResponse(createComment(submission, content, CommentTypeConstain.COMMENT));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    // Hàm getComments: Nhận các tham số lọc/phân trang của getComments, truy vấn dữ liệu phù hợp từ repository, ánh xạ
    // từng entity sang DTO và trả về cho giao diện.
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
    // Hàm updateComment: Nhận mã bản ghi cùng dữ liệu cập nhật của updateComment, tải bản ghi hiện có, kiểm tra trạng
    // thái và ràng buộc rồi ghi các giá trị mới xuống repository.
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
    // Hàm deleteComment: Nhận mã bản ghi của deleteComment, kiểm tra quyền và các quan hệ đang sử dụng, sau đó xóa hoặc
    // chuyển bản ghi sang trạng thái tương ứng.
    public void deleteComment(Long commentId) {
        CommentEntity comment = findComment(commentId);
        requireOwnerOrAdmin(comment);
        requireOrdinaryComment(comment);
        comment.setDeletedAt(LocalDateTime.now());
        commentRepository.save(comment);
    }

    @Transactional
    // Hàm createWorkflowComment: Nhận dữ liệu đầu vào của createWorkflowComment, kiểm tra các trường bắt buộc và quan
    // hệ liên quan, tạo bản ghi nghiệp vụ rồi lưu repository để trả kết quả cho API.
    public CommentEntity createWorkflowComment(
            SubmistionEntity submission, String content, CommentTypeConstain commentType) {
        validateContent(content);
        requireReviewAccess(submission);
        if (commentType == CommentTypeConstain.COMMENT) {
            throw new AppException(ErrorCode.COMMENT_OPERATION_NOT_ALLOWED);
        }
        return createComment(submission, content, commentType);
    }

    // Hàm createComment: Nhận dữ liệu đầu vào của createComment, kiểm tra các trường bắt buộc và quan hệ liên quan, tạo
    // bản ghi nghiệp vụ rồi lưu repository để trả kết quả cho API.
    private CommentEntity createComment(SubmistionEntity submission, String content, CommentTypeConstain commentType) {
        UserEntity author = currentUser();
        return commentRepository.save(CommentEntity.builder()
                .content(content.trim())
                .commentType(commentType)
                .submistion(submission)
                .createdBy(author)
                .edited(false)
                .build());
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireOrdinaryComment(CommentEntity comment) {
        if (comment.getCommentType() != CommentTypeConstain.COMMENT) {
            throw new AppException(ErrorCode.COMMENT_OPERATION_NOT_ALLOWED);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireOwnerOrAdmin(CommentEntity comment) {
        String userId = currentAuthentication().getName();
        if (!hasAuthority("ROLE_ADMIN")
                && (comment.getCreatedBy() == null
                        || !userId.equals(comment.getCreatedBy().getUserId()))) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireReadAccess(SubmistionEntity submission) {
        if (isManager()
                || isTeamMember(submission.getTeam())
                || isAssignedSupervisor(submission.getTeam())
                || isAssignedCommitteeMember(submission.getTeam())) {
            return;
        }
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireReviewAccess(SubmistionEntity submission) {
        if (isManager()
                || isAssignedSupervisor(submission.getTeam())
                || isAssignedCommitteeMember(submission.getTeam())) {
            return;
        }
        throw new AppException(ErrorCode.ACCESS_DENIED);
    }

    /**
     * Đối chiếu giảng viên hiện tại với thành viên ACTIVE của hội đồng đang
     * được xếp lịch cho đề tài của nhóm để cấp quyền nhận xét tại buổi bảo vệ.
     */
    private boolean isAssignedCommitteeMember(TeamEntity team) {
        if (team == null
                || team.getTopic() == null
                || team.getTopic().getIdTopic() == null
                || defenseScheduleRepository == null
                || lectureRepository == null) {
            return false;
        }
        String userId = currentAuthentication().getName();
        return lectureRepository
                .findByUser_UserId(userId)
                .map(lecture -> defenseScheduleRepository.existsActiveCommitteeMemberForTopic(
                        team.getTopic().getIdTopic(),
                        lecture.getLectureId(),
                        CommitteeMemberStatusConstain.ACTIVE,
                        DefenseScheduleStatusConstain.CANCELLED))
                .orElse(false);
    }

    // Hàm isTeamMember: Đối chiếu userId hiện tại với danh sách sinh viên của nhóm đề tài để xác định người dùng có
    // thuộc nhóm hay không.
    private boolean isTeamMember(TeamEntity team) {
        String userId = currentAuthentication().getName();
        return team.getStudentEntities().stream()
                .anyMatch(student -> student.getUserEntity() != null
                        && userId.equals(student.getUserEntity().getUserId()));
    }

    // Hàm isAssignedSupervisor: Kiểm tra danh sách phân công của đề tài có giảng viên hiện tại ở trạng thái ACTIVE, từ
    // đó xác định quyền giảng viên hướng dẫn.
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

    // Hàm isManager: Kiểm tra Authentication hiện tại có quyền ROLE_ADMIN hoặc ROLE_FACULTY hay không để xác định người
    // dùng có quyền quản lý dữ liệu.
    private boolean isManager() {
        return hasAuthority("ROLE_ADMIN") || hasAuthority("ROLE_FACULTY");
    }

    // Hàm hasAuthority: Nhận tên quyền cần kiểm tra; so sánh với danh sách GrantedAuthority của tài khoản hiện tại và
    // trả về true nếu tài khoản có quyền đó.
    private boolean hasAuthority(String authority) {
        return currentAuthentication().getAuthorities().stream()
                .anyMatch(item -> item.getAuthority().equals(authority));
    }

    // Hàm currentUser: Lấy userId của tài khoản đang đăng nhập từ Authentication, truy vấn UserEntity tương ứng và trả
    // về người thực hiện để gắn vào bản ghi.
    private UserEntity currentUser() {
        return userRepository
                .findById(currentAuthentication().getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    // Hàm currentAuthentication: Đọc Authentication từ SecurityContext của request hiện tại; từ chối khi chưa đăng nhập
    // và trả về đối tượng xác thực để lấy userId cùng quyền.
    private Authentication currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication;
    }

    // Hàm findSubmission: Nhận mã hoặc điều kiện tìm kiếm của findSubmission, truy vấn bản ghi/quan hệ tương ứng, báo
    // lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    private SubmistionEntity findSubmission(Long submissionId) {
        return submissionRepository
                .findById(submissionId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));
    }

    // Hàm findComment: Nhận mã hoặc điều kiện tìm kiếm của findComment, truy vấn bản ghi/quan hệ tương ứng, báo lỗi khi
    // không tồn tại và trả về dữ liệu đã ánh xạ.
    private CommentEntity findComment(Long commentId) {
        return commentRepository
                .findActiveById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new AppException(ErrorCode.SUBMISSION_COMMENT_NOT_BLANK);
        }
    }
}

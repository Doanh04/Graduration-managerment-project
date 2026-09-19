package com.graduration.Service.ManagerService;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.Constain.DefenseScheduleStatusConstain;
import com.graduration.Constain.ReviewAssignmentStatusConstain;
import com.graduration.Constain.ReviewRecommendationConstain;
import com.graduration.Constain.StatusConstain;
import com.graduration.Constain.SupervisorAssignmentStatusConstain;
import com.graduration.Constain.TopicStatusConstain;
import com.graduration.DTO.Request.AssignReviewRequest;
import com.graduration.DTO.Request.CancelReviewRequest;
import com.graduration.DTO.Request.SubmitReviewRequest;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.ReviewAssignmentResponse;
import com.graduration.Repository.LectureRepository;
import com.graduration.Repository.ReviewAssignmentRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.LectureEntity;
import com.graduration.entity.ReviewAssignmentEntity;
import com.graduration.entity.TopicEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.ReviewAssignmentMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewAssignmentService {
    final ReviewAssignmentRepository reviewRepository;
    final TopicRepository topicRepository;
    final LectureRepository lectureRepository;
    final UserRepository userRepository;
    final ReviewAssignmentMapper reviewMapper;

    @Value("${app.review.max-reviewers-per-topic:1}")
    int maxReviewersPerTopic;

    @Value("${app.review.max-assignments-per-period:5}")
    int maxAssignmentsPerPeriod;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm assign: Nhận dữ liệu đầu vào của assign, kiểm tra các trường bắt buộc và quan hệ liên quan, tạo bản ghi
    // nghiệp vụ rồi lưu repository để trả kết quả cho API.
    public ReviewAssignmentResponse assign(Long topicId, AssignReviewRequest request) {
        TopicEntity topic = findEligibleTopic(topicId);
        LectureEntity lecture = lectureRepository
                .findById(request.getLectureId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        requireActiveLecturer(lecture);
        requireValidDeadline(topic, request.getDeadline());
        if (isActiveSupervisor(topic, lecture.getLectureId())) {
            throw new AppException(ErrorCode.REVIEW_SUPERVISOR_CONFLICT);
        }
        if (reviewRepository.existsActiveAssignment(
                topicId, lecture.getLectureId(), ReviewAssignmentStatusConstain.CANCELLED)) {
            throw new AppException(ErrorCode.REVIEW_ASSIGNMENT_ALREADY_EXISTS);
        }
        if (reviewRepository.countForTopic(topicId, ReviewAssignmentStatusConstain.CANCELLED) >= maxReviewersPerTopic) {
            throw new AppException(ErrorCode.REVIEW_ASSIGNMENT_LIMIT_REACHED);
        }
        if (reviewRepository.countForLecturerInPeriod(
                        lecture.getLectureId(),
                        topic.getDefensePeriod().getID_Defense(),
                        ReviewAssignmentStatusConstain.CANCELLED)
                >= maxAssignmentsPerPeriod) {
            throw new AppException(ErrorCode.REVIEW_ASSIGNMENT_LIMIT_REACHED);
        }
        ReviewAssignmentEntity assignment = ReviewAssignmentEntity.builder()
                .topic(topic)
                .lecture(lecture)
                .deadline(request.getDeadline())
                .status(ReviewAssignmentStatusConstain.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .assignedBy(currentUser())
                .note(normalize(request.getNote()))
                .build();
        return reviewMapper.toResponse(reviewRepository.save(assignment));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    // Hàm getByTopic: Nhận các tham số lọc/phân trang của getByTopic, truy vấn dữ liệu phù hợp từ repository, ánh xạ
    // từng entity sang DTO và trả về cho giao diện.
    public PageResponse<ReviewAssignmentResponse> getByTopic(
            Long topicId, ReviewAssignmentStatusConstain status, Integer page, Integer size) {
        if (!topicRepository.existsById(topicId)) {
            throw new AppException(ErrorCode.TOPIC_NOT_FOUND);
        }
        return PageResponse.from(
                reviewRepository.findByTopic(topicId, status, PaginationSupport.pageRequest(page, size)),
                reviewMapper::toResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    // Hàm getByLecturer: Nhận các tham số lọc/phân trang của getByLecturer, truy vấn dữ liệu phù hợp từ repository, ánh
    // xạ từng entity sang DTO và trả về cho giao diện.
    public PageResponse<ReviewAssignmentResponse> getByLecturer(
            String lectureId, ReviewAssignmentStatusConstain status, Integer page, Integer size) {
        if (!lectureRepository.existsById(lectureId)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return pageByLecturer(lectureId, status, page, size);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_REVIEWER', 'ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    // Hàm getMine: Nhận các tham số lọc/phân trang của getMine, truy vấn dữ liệu phù hợp từ repository, ánh xạ từng
    // entity sang DTO và trả về cho giao diện.
    public PageResponse<ReviewAssignmentResponse> getMine(
            ReviewAssignmentStatusConstain status, Integer page, Integer size) {
        LectureEntity lecture = currentLecturer();
        return pageByLecturer(lecture.getLectureId(), status, page, size);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_REVIEWER', 'ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm start: Nhận mã bản ghi và thông tin thao tác của start, kiểm tra trạng thái hiện tại cùng quyền thực hiện,
    // cập nhật trạng thái/lý do và lưu thay đổi.
    public ReviewAssignmentResponse start(Long assignmentId) {
        ReviewAssignmentEntity assignment = findAssignment(assignmentId);
        requireOwnerOrManager(assignment);
        requireStatus(assignment, ReviewAssignmentStatusConstain.ASSIGNED);
        assignment.setStatus(ReviewAssignmentStatusConstain.IN_PROGRESS);
        return reviewMapper.toResponse(reviewRepository.save(assignment));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_REVIEWER', 'ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm submit: Nhận assignmentId và SubmitReviewRequest; kiểm tra người phụ trách, trạng thái, nhận xét và
    // recommendation, ghi thời điểm gửi rồi chuyển assignment sang SUBMITTED.
    public ReviewAssignmentResponse submit(Long assignmentId, SubmitReviewRequest request) {
        ReviewAssignmentEntity assignment = findAssignment(assignmentId);
        requireOwnerOrManager(assignment);
        if (assignment.getStatus() != ReviewAssignmentStatusConstain.ASSIGNED
                && assignment.getStatus() != ReviewAssignmentStatusConstain.IN_PROGRESS
                && assignment.getStatus() != ReviewAssignmentStatusConstain.REVISION_REQUIRED) {
            throw new AppException(ErrorCode.REVIEW_OPERATION_NOT_ALLOWED);
        }
        if (request == null
                || request.getReviewComment() == null
                || request.getReviewComment().isBlank()) {
            throw new AppException(ErrorCode.REVIEW_COMMENT_NOT_BLANK);
        }
        if (request.getRecommendation() == null) {
            throw new AppException(ErrorCode.REVIEW_RECOMMENDATION_NOT_BLANK);
        }
        assignment.setReviewComment(request.getReviewComment().trim());
        assignment.setRecommendation(request.getRecommendation());
        assignment.setSubmittedAt(LocalDateTime.now());
        assignment.setStatus(ReviewAssignmentStatusConstain.SUBMITTED);
        return reviewMapper.toResponse(reviewRepository.save(assignment));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm approve: Nhận mã bản ghi và thông tin thao tác của approve, kiểm tra trạng thái hiện tại cùng quyền thực
    // hiện, cập nhật trạng thái/lý do và lưu thay đổi.
    public ReviewAssignmentResponse approve(Long assignmentId) {
        ReviewAssignmentEntity assignment = findAssignment(assignmentId);
        requireStatus(assignment, ReviewAssignmentStatusConstain.SUBMITTED);
        if (assignment.getRecommendation() != ReviewRecommendationConstain.ELIGIBLE_FOR_DEFENSE) {
            throw new AppException(ErrorCode.REVIEW_OPERATION_NOT_ALLOWED);
        }
        assignment.setStatus(ReviewAssignmentStatusConstain.APPROVED);
        assignment.setReviewedAt(LocalDateTime.now());
        assignment.setReviewedBy(currentUser());
        return reviewMapper.toResponse(reviewRepository.save(assignment));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm requestRevision: Nhận assignment và yêu cầu chỉnh sửa; kiểm tra người sở hữu cùng trạng thái, lưu ghi chú/lý
    // do yêu cầu bổ sung và chuyển assignment về trạng thái cần xử lý lại.
    public ReviewAssignmentResponse requestRevision(Long assignmentId) {
        ReviewAssignmentEntity assignment = findAssignment(assignmentId);
        requireStatus(assignment, ReviewAssignmentStatusConstain.SUBMITTED);
        assignment.setStatus(ReviewAssignmentStatusConstain.REVISION_REQUIRED);
        assignment.setReviewedAt(LocalDateTime.now());
        assignment.setReviewedBy(currentUser());
        return reviewMapper.toResponse(reviewRepository.save(assignment));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm cancel: Nhận mã bản ghi của cancel, kiểm tra quyền và các quan hệ đang sử dụng, sau đó xóa hoặc chuyển bản
    // ghi sang trạng thái tương ứng.
    public ReviewAssignmentResponse cancel(Long assignmentId, CancelReviewRequest request) {
        ReviewAssignmentEntity assignment = findAssignment(assignmentId);
        if (assignment.getStatus() == ReviewAssignmentStatusConstain.CANCELLED
                || assignment.getStatus() == ReviewAssignmentStatusConstain.APPROVED) {
            throw new AppException(ErrorCode.REVIEW_OPERATION_NOT_ALLOWED);
        }
        if (assignment.getTopic().getDefenseSchedule() != null
                && assignment.getTopic().getDefenseSchedule().getStatus() == DefenseScheduleStatusConstain.COMPLETED) {
            throw new AppException(ErrorCode.REVIEW_OPERATION_NOT_ALLOWED);
        }
        if (request == null
                || request.getReason() == null
                || request.getReason().isBlank()) {
            throw new AppException(ErrorCode.REVIEW_CANCEL_REASON_NOT_BLANK);
        }
        assignment.setStatus(ReviewAssignmentStatusConstain.CANCELLED);
        assignment.setCancelledAt(LocalDateTime.now());
        assignment.setCancelledReason(request.getReason().trim());
        return reviewMapper.toResponse(reviewRepository.save(assignment));
    }

    // Hàm pageByLecturer: Nhận lectureId cùng thông tin phân trang; truy vấn các assignment phản biện của giảng viên đó
    // và chuyển từng entity thành danh sách response phân trang.
    private PageResponse<ReviewAssignmentResponse> pageByLecturer(
            String lectureId, ReviewAssignmentStatusConstain status, Integer page, Integer size) {
        return PageResponse.from(
                reviewRepository.findByLecturer(lectureId, status, PaginationSupport.pageRequest(page, size)),
                reviewMapper::toResponse);
    }

    // Hàm findEligibleTopic: Nhận mã hoặc điều kiện tìm kiếm của findEligibleTopic, truy vấn bản ghi/quan hệ tương ứng,
    // báo lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    private TopicEntity findEligibleTopic(Long topicId) {
        TopicEntity topic =
                topicRepository.findById(topicId).orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
        if (topic.getDefensePeriod() == null
                || topic.getDefensePeriod().getStatus() == DefensePeriodConstain.FINISHED) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_FINISHED);
        }
        if (topic.getTeam() == null
                || (topic.getStatus() != TopicStatusConstain.APPROVED
                        && topic.getStatus() != TopicStatusConstain.REGISTERED
                        && topic.getStatus() != TopicStatusConstain.IN_PROGRESS)) {
            throw new AppException(ErrorCode.DEFENSE_SCHEDULE_TOPIC_NOT_ELIGIBLE);
        }
        return topic;
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireValidDeadline(TopicEntity topic, LocalDateTime deadline) {
        if (deadline == null || !deadline.isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.REVIEW_DEADLINE_INVALID);
        }
        if (topic.getDefensePeriod().getEndDate() != null
                && deadline.toLocalDate().isAfter(topic.getDefensePeriod().getEndDate())) {
            throw new AppException(ErrorCode.REVIEW_DEADLINE_INVALID);
        }
        if (topic.getDefenseSchedule() != null
                && !deadline.isBefore(LocalDateTime.of(
                        topic.getDefenseSchedule().getDefenseDate(),
                        topic.getDefenseSchedule().getStartTime()))) {
            throw new AppException(ErrorCode.REVIEW_DEADLINE_INVALID);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireActiveLecturer(LectureEntity lecture) {
        StatusConstain status =
                lecture.getUser() == null ? null : lecture.getUser().getStatus();
        if (lecture.getUser() == null || status == StatusConstain.INACTIVE || status == StatusConstain.DELETED) {
            throw new AppException(ErrorCode.LECTURER_INACTIVE);
        }
    }

    // Hàm isActiveSupervisor: Duyệt các phân công của đề tài và chỉ chấp nhận phân công ACTIVE trùng lectureId được
    // truyền vào để xác định giảng viên hướng dẫn hiện hành.
    private boolean isActiveSupervisor(TopicEntity topic, String lectureId) {
        return topic.getTopicSuperVisorEntities().stream()
                .anyMatch(item -> item.getStatus() == SupervisorAssignmentStatusConstain.ACTIVE
                        && item.getLecture() != null
                        && lectureId.equals(item.getLecture().getLectureId()));
    }

    // Hàm findAssignment: Nhận mã hoặc điều kiện tìm kiếm của findAssignment, truy vấn bản ghi/quan hệ tương ứng, báo
    // lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    private ReviewAssignmentEntity findAssignment(Long assignmentId) {
        return reviewRepository
                .findById(assignmentId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_ASSIGNMENT_NOT_FOUND));
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireOwnerOrManager(ReviewAssignmentEntity assignment) {
        if (isManager()) {
            return;
        }
        String userId = currentAuthentication().getName();
        if (assignment.getLecture().getUser() == null
                || !userId.equals(assignment.getLecture().getUser().getUserId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireStatus(ReviewAssignmentEntity assignment, ReviewAssignmentStatusConstain expected) {
        if (assignment.getStatus() != expected) {
            throw new AppException(ErrorCode.REVIEW_OPERATION_NOT_ALLOWED);
        }
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

    // Hàm currentLecturer: Dùng userId hiện tại truy vấn hồ sơ giảng viên; nếu không có hồ sơ thì báo lỗi, còn có thì
    // trả về LectureEntity để thực hiện nghiệp vụ.
    private LectureEntity currentLecturer() {
        return lectureRepository
                .findByUser_UserId(currentAuthentication().getName())
                .orElseThrow(() -> new AppException(ErrorCode.LECTURER_PROFILE_NOT_FOUND));
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

    // Hàm normalize: Nhận ghi chú phân công phản biện; chuyển chuỗi trống thành null và trim nội dung trước khi lưu
    // ReviewAssignmentEntity.
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

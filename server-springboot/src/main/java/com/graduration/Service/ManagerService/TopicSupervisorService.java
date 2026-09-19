package com.graduration.Service.ManagerService;

import java.time.LocalDateTime;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.Constain.StatusConstain;
import com.graduration.Constain.SupervisorAssignmentStatusConstain;
import com.graduration.Constain.SupervisorRoleConstain;
import com.graduration.Constain.TopicStatusConstain;
import com.graduration.DTO.Request.AssignTopicSupervisorRequest;
import com.graduration.DTO.Request.DeactivateTopicSupervisorRequest;
import com.graduration.DTO.Request.UpdateTopicSupervisorRequest;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.TopicSupervisorResponse;
import com.graduration.Repository.LectureRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.Repository.TopicSupervisorRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.LectureEntity;
import com.graduration.entity.TopicEntity;
import com.graduration.entity.TopicSuperVisorEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.TopicSupervisorMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopicSupervisorService {
    final TopicSupervisorRepository supervisorRepository;
    final TopicRepository topicRepository;
    final LectureRepository lectureRepository;
    final UserRepository userRepository;
    final TopicSupervisorMapper supervisorMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm assign: Nhận dữ liệu đầu vào của assign, kiểm tra các trường bắt buộc và quan hệ liên quan, tạo bản ghi
    // nghiệp vụ rồi lưu repository để trả kết quả cho API.
    public TopicSupervisorResponse assign(Long topicId, AssignTopicSupervisorRequest request) {
        TopicEntity topic = findApprovedAssignableTopic(topicId);
        LectureEntity lecture = findLecturer(request.getLectureId());
        requireActiveLecturer(lecture);
        if (supervisorRepository.existsByTopic_IdTopicAndLecture_LectureIdAndStatus(
                topicId, lecture.getLectureId(), SupervisorAssignmentStatusConstain.ACTIVE)) {
            throw new AppException(ErrorCode.TOPIC_SUPERVISOR_ALREADY_ASSIGNED);
        }
        requirePrimaryAvailable(topicId, SupervisorRoleConstain.PRIMARY, null);
        TopicSuperVisorEntity assignment = TopicSuperVisorEntity.builder()
                .topic(topic)
                .lecture(lecture)
                .supervisorRole(SupervisorRoleConstain.PRIMARY)
                .status(SupervisorAssignmentStatusConstain.ACTIVE)
                .assignedAt(LocalDateTime.now())
                .assignedBy(currentUser())
                .note(normalize(request.getNote()))
                .build();
        return supervisorMapper.toResponse(supervisorRepository.save(assignment));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    // Hàm getByTopic: Nhận các tham số lọc/phân trang của getByTopic, truy vấn dữ liệu phù hợp từ repository, ánh xạ
    // từng entity sang DTO và trả về cho giao diện.
    public PageResponse<TopicSupervisorResponse> getByTopic(Long topicId, Integer page, Integer size) {
        if (!topicRepository.existsById(topicId)) {
            throw new AppException(ErrorCode.TOPIC_NOT_FOUND);
        }
        return PageResponse.from(
                supervisorRepository.findByTopic_IdTopic(topicId, PaginationSupport.pageRequest(page, size)),
                supervisorMapper::toResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    // Hàm getAll: Nhận các tham số lọc/phân trang của getAll, truy vấn dữ liệu phù hợp từ repository, ánh xạ từng
    // entity sang DTO và trả về cho giao diện.
    public PageResponse<TopicSupervisorResponse> getAll(Integer page, Integer size) {
        return PageResponse.from(
                supervisorRepository.findAll(PaginationSupport.pageRequest(page, size)), supervisorMapper::toResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    // Hàm getByLecturer: Nhận các tham số lọc/phân trang của getByLecturer, truy vấn dữ liệu phù hợp từ repository, ánh
    // xạ từng entity sang DTO và trả về cho giao diện.
    public PageResponse<TopicSupervisorResponse> getByLecturer(String lectureId, Integer page, Integer size) {
        if (!lectureRepository.existsById(lectureId)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return activeByLecturer(lectureId, page, size);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_SUPERVISOR', 'ROLE_FACULTY', 'ROLE_ADMIN')")
    @Transactional(readOnly = true)
    // Hàm getMine: Nhận các tham số lọc/phân trang của getMine, truy vấn dữ liệu phù hợp từ repository, ánh xạ từng
    // entity sang DTO và trả về cho giao diện.
    public PageResponse<TopicSupervisorResponse> getMine(Integer page, Integer size) {
        LectureEntity lecture = lectureRepository
                .findByUser_UserId(currentAuthentication().getName())
                .orElseThrow(() -> new AppException(ErrorCode.LECTURER_PROFILE_NOT_FOUND));
        return activeByLecturer(lecture.getLectureId(), page, size);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm update: Nhận mã bản ghi cùng dữ liệu cập nhật của update, tải bản ghi hiện có, kiểm tra trạng thái và ràng
    // buộc rồi ghi các giá trị mới xuống repository.
    public TopicSupervisorResponse update(Long assignmentId, UpdateTopicSupervisorRequest request) {
        TopicSuperVisorEntity assignment = findAssignment(assignmentId);
        requireActive(assignment);
        findAssignableTopic(assignment.getTopic().getIdTopic());
        requirePrimaryAvailable(assignment.getTopic().getIdTopic(), SupervisorRoleConstain.PRIMARY, assignmentId);
        assignment.setSupervisorRole(SupervisorRoleConstain.PRIMARY);
        assignment.setNote(normalize(request.getNote()));
        return supervisorMapper.toResponse(supervisorRepository.save(assignment));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm deactivate: Nhận mã đối tượng cùng lý do vô hiệu hóa; kiểm tra trạng thái hiện tại và ràng buộc đang sử dụng,
    // ghi lý do, đổi sang INACTIVE rồi lưu.
    public TopicSupervisorResponse deactivate(Long assignmentId, DeactivateTopicSupervisorRequest request) {
        TopicSuperVisorEntity assignment = findAssignment(assignmentId);
        requireActive(assignment);
        if (request == null
                || request.getReason() == null
                || request.getReason().isBlank()) {
            throw new AppException(ErrorCode.SUPERVISOR_DEACTIVATION_REASON_NOT_BLANK);
        }
        assignment.setStatus(SupervisorAssignmentStatusConstain.INACTIVE);
        assignment.setEndedAt(LocalDateTime.now());
        assignment.setNote(appendReason(assignment.getNote(), request.getReason()));
        return supervisorMapper.toResponse(supervisorRepository.save(assignment));
    }

    // Hàm activeByLecturer: Nhận lectureId cùng tham số phân trang; truy vấn các phân công hướng dẫn ACTIVE của giảng
    // viên và ánh xạ thành PageResponse.
    private PageResponse<TopicSupervisorResponse> activeByLecturer(String lectureId, Integer page, Integer size) {
        return PageResponse.from(
                supervisorRepository.findByLecture_LectureIdAndStatus(
                        lectureId,
                        SupervisorAssignmentStatusConstain.ACTIVE,
                        PaginationSupport.pageRequest(page, size)),
                supervisorMapper::toResponse);
    }

    // Hàm findAssignableTopic: Nhận mã hoặc điều kiện tìm kiếm của findAssignableTopic, truy vấn bản ghi/quan hệ tương
    // ứng, báo lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    private TopicEntity findAssignableTopic(Long topicId) {
        TopicEntity topic =
                topicRepository.findById(topicId).orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
        if (topic.getDefensePeriod() == null
                || topic.getDefensePeriod().getStatus() == DefensePeriodConstain.FINISHED) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_FINISHED);
        }
        if (topic.getStatus() == TopicStatusConstain.REJECTED || topic.getStatus() == TopicStatusConstain.CANCELLED) {
            throw new AppException(ErrorCode.TOPIC_OPERATION_NOT_ALLOWED);
        }
        return topic;
    }

    // Hàm findApprovedAssignableTopic: Nhận mã hoặc điều kiện tìm kiếm của findApprovedAssignableTopic, truy vấn bản
    // ghi/quan hệ tương ứng, báo lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    private TopicEntity findApprovedAssignableTopic(Long topicId) {
        TopicEntity topic = findAssignableTopic(topicId);
        if (topic.getStatus() != TopicStatusConstain.APPROVED
                && topic.getStatus() != TopicStatusConstain.REGISTERED
                && topic.getStatus() != TopicStatusConstain.IN_PROGRESS) {
            throw new AppException(ErrorCode.TOPIC_NOT_AVAILABLE);
        }
        return topic;
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireActiveLecturer(LectureEntity lecture) {
        StatusConstain status =
                lecture.getUser() == null ? null : lecture.getUser().getStatus();
        if (lecture.getUser() == null || status == StatusConstain.INACTIVE || status == StatusConstain.DELETED) {
            throw new AppException(ErrorCode.LECTURER_INACTIVE);
        }
    }

    // Hàm findLecturer: Nhận mã hoặc điều kiện tìm kiếm của findLecturer, truy vấn bản ghi/quan hệ tương ứng, báo lỗi
    // khi không tồn tại và trả về dữ liệu đã ánh xạ.
    private LectureEntity findLecturer(String identifier) {
        return lectureRepository
                .findById(identifier)
                .or(() -> lectureRepository.findByLectureCode(identifier))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requirePrimaryAvailable(Long topicId, SupervisorRoleConstain role, Long excludedAssignmentId) {
        if (role != SupervisorRoleConstain.PRIMARY) {
            return;
        }
        boolean exists = excludedAssignmentId == null
                ? supervisorRepository.existsByTopic_IdTopicAndSupervisorRoleAndStatus(
                        topicId, SupervisorRoleConstain.PRIMARY, SupervisorAssignmentStatusConstain.ACTIVE)
                : supervisorRepository.existsByTopic_IdTopicAndSupervisorRoleAndStatusAndIdSuperVisorNot(
                        topicId,
                        SupervisorRoleConstain.PRIMARY,
                        SupervisorAssignmentStatusConstain.ACTIVE,
                        excludedAssignmentId);
        if (exists) {
            throw new AppException(ErrorCode.TOPIC_PRIMARY_SUPERVISOR_ALREADY_EXISTS);
        }
    }

    // Hàm findAssignment: Nhận mã hoặc điều kiện tìm kiếm của findAssignment, truy vấn bản ghi/quan hệ tương ứng, báo
    // lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    private TopicSuperVisorEntity findAssignment(Long assignmentId) {
        return supervisorRepository
                .findById(assignmentId)
                .orElseThrow(() -> new AppException(ErrorCode.TOPIC_SUPERVISOR_NOT_FOUND));
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireActive(TopicSuperVisorEntity assignment) {
        if (assignment.getStatus() != SupervisorAssignmentStatusConstain.ACTIVE) {
            throw new AppException(ErrorCode.SUPERVISOR_ASSIGNMENT_NOT_ACTIVE);
        }
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

    // Hàm normalize: Nhận ghi chú phân công giảng viên hướng dẫn; chuyển ghi chú trống thành null và trim nội dung để
    // lưu TopicSuperVisorEntity.
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // Hàm appendReason: Nhận ghi chú hiện có và lý do vô hiệu hóa; trim lý do rồi nối vào ghi chú theo dấu phân cách để
    // bảo toàn lịch sử nguyên nhân.
    private String appendReason(String note, String reason) {
        String normalizedReason = reason.trim();
        return note == null || note.isBlank()
                ? "Deactivated: " + normalizedReason
                : note.trim() + " | Deactivated: " + normalizedReason;
    }
}

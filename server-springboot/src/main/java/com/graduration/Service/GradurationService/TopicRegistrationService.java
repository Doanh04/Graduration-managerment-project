package com.graduration.Service.GradurationService;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.TopicRegistrationStatusConstain;
import com.graduration.Constain.TopicStatusConstain;
import com.graduration.DTO.Request.CreateTopicRegistrationRequest;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.TopicRegistrationResponse;
import com.graduration.Repository.GraduationEnrollmentRepository;
import com.graduration.Repository.LectureRepository;
import com.graduration.Repository.TeamRepository;
import com.graduration.Repository.TopicRegistrationRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.GraduationEnrollmentEntity;
import com.graduration.entity.LectureEntity;
import com.graduration.entity.StudentEntity;
import com.graduration.entity.TeamEntity;
import com.graduration.entity.TopicEntity;
import com.graduration.entity.TopicRegistrationEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TopicRegistrationService {
    TopicRegistrationRepository registrationRepository;
    GraduationEnrollmentRepository enrollmentRepository;
    TopicRepository topicRepository;
    TeamRepository teamRepository;
    LectureRepository lectureRepository;
    UserRepository userRepository;
    GraduationEnrollmentService graduationEnrollmentService;

    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Transactional
    // Hàm register: Nhận dữ liệu đầu vào của register, kiểm tra các trường bắt buộc và quan hệ liên quan, tạo bản ghi
    // nghiệp vụ rồi lưu repository để trả kết quả cho API.
    public TopicRegistrationResponse register(CreateTopicRegistrationRequest request) {
        StudentEntity student = currentStudent();
        TeamEntity team = teamRepository
                .findByStudentEntities_UserEntity_UserId(currentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_TEAM_REQUIRED));
        requireTeamWithoutTopic(team);
        TopicEntity topic = topicRepository
                .findById(request.getTopicId())
                .orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
        if (topic.getStatus() != TopicStatusConstain.APPROVED
                || topic.getTeam() != null
                || teamRepository.existsByTopic_IdTopic(topic.getIdTopic())) {
            throw new AppException(ErrorCode.TOPIC_NOT_AVAILABLE);
        }
        GraduationEnrollmentEntity enrollment = enrollmentRepository
                .findByStudent_IdStudentAndDefensePeriod_ID_Defense(
                        student.getIdStudent(), topic.getDefensePeriod().getID_Defense())
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_PERIOD_MISMATCH));
        graduationEnrollmentService.requireParticipationAllowed(
                student.getIdStudent(), topic.getDefensePeriod().getID_Defense());
        if (registrationRepository.existsByEnrollment_EnrollmentIdAndStatus(
                        enrollment.getEnrollmentId(), TopicRegistrationStatusConstain.PENDING)
                || !registrationRepository
                        .findByTeam_IdTeamAndStatus(team.getIdTeam(), TopicRegistrationStatusConstain.PENDING)
                        .isEmpty()) {
            throw new AppException(ErrorCode.TOPIC_REGISTRATION_ALREADY_PENDING);
        }
        if (registrationRepository.existsByEnrollment_EnrollmentIdAndTopic_IdTopicAndStatus(
                enrollment.getEnrollmentId(), topic.getIdTopic(), TopicRegistrationStatusConstain.APPROVED)) {
            throw new AppException(ErrorCode.TEAM_ALREADY_HAS_TOPIC);
        }
        LectureEntity preferred = request.getPreferredSupervisorId() == null
                        || request.getPreferredSupervisorId().isBlank()
                ? null
                : lectureRepository
                        .findById(request.getPreferredSupervisorId())
                        .orElseThrow(() -> new AppException(ErrorCode.LECTURER_PROFILE_NOT_FOUND));
        int priority = request.getPriority() == null || request.getPriority() < 1 ? 1 : request.getPriority();
        TopicRegistrationEntity registration = registrationRepository
                .findByEnrollment_EnrollmentIdAndPriority(enrollment.getEnrollmentId(), priority)
                .orElseGet(() -> TopicRegistrationEntity.builder()
                        .enrollment(enrollment)
                        .priority(priority)
                        .build());
        registration.setTopic(topic);
        registration.setTeam(team);
        registration.setPreferredSupervisor(preferred);
        registration.setStatus(TopicRegistrationStatusConstain.PENDING);
        registration.setNote(normalize(request.getNote()));
        registration.setSubmittedAt(LocalDateTime.now());
        registration.setReviewedAt(null);
        registration.setReviewedBy(null);
        registration.setRejectionReason(null);
        return toResponse(registrationRepository.save(registration));
    }

    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Transactional(readOnly = true)
    // Hàm getMine: Nhận các tham số lọc/phân trang của getMine, truy vấn dữ liệu phù hợp từ repository, ánh xạ từng
    // entity sang DTO và trả về cho giao diện.
    public List<TopicRegistrationResponse> getMine() {
        TeamEntity team = teamRepository
                .findByStudentEntities_UserEntity_UserId(currentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_TEAM_REQUIRED));
        return registrationRepository.findByTeam_IdTeamOrderBySubmittedAtDesc(team.getIdTeam()).stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    // Hàm getAll: Nhận các tham số lọc/phân trang của getAll, truy vấn dữ liệu phù hợp từ repository, ánh xạ từng
    // entity sang DTO và trả về cho giao diện.
    public PageResponse<TopicRegistrationResponse> getAll(
            TopicRegistrationStatusConstain status, Integer page, Integer size) {
        var pageable = PaginationSupport.pageRequest(page, size);
        Page<TopicRegistrationEntity> result = status == null
                ? registrationRepository.findAllByOrderBySubmittedAtDesc(pageable)
                : registrationRepository.findByStatusOrderBySubmittedAtDesc(status, pageable);
        return PageResponse.from(result, this::toResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm approve: Nhận mã bản ghi và thông tin thao tác của approve, kiểm tra trạng thái hiện tại cùng quyền thực
    // hiện, cập nhật trạng thái/lý do và lưu thay đổi.
    public TopicRegistrationResponse approve(Long registrationId) {
        TopicRegistrationEntity registration = findRegistration(registrationId);
        requirePending(registration);
        TeamEntity team = registration.getTeam();
        TopicEntity topic = registration.getTopic();
        requireTeamWithoutTopic(team);
        if (topic.getStatus() != TopicStatusConstain.APPROVED
                || topic.getTeam() != null
                || teamRepository.existsByTopic_IdTopic(topic.getIdTopic())) {
            throw new AppException(ErrorCode.TOPIC_NOT_AVAILABLE);
        }
        team.setTopic(topic);
        topic.setTeam(team);
        topic.setStatus(TopicStatusConstain.APPROVED);
        topicRepository.save(topic);
        teamRepository.save(team);
        graduationEnrollmentService.autoEnrollStudents(
                team.getStudentEntities(), topic.getDefensePeriod(), "Tự động ghi danh khi Admin duyệt đăng ký đề tài");
        registration.setStatus(TopicRegistrationStatusConstain.APPROVED);
        review(registration, null);
        registrationRepository
                .findByTeam_IdTeamAndStatus(team.getIdTeam(), TopicRegistrationStatusConstain.PENDING)
                .stream()
                .filter(item -> !item.getRegistrationId().equals(registrationId))
                .forEach(item -> {
                    item.setStatus(TopicRegistrationStatusConstain.CANCELLED);
                    review(item, "Nhóm đã được duyệt một đề tài khác");
                });
        return toResponse(registrationRepository.save(registration));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm reject: Nhận mã bản ghi và thông tin thao tác của reject, kiểm tra trạng thái hiện tại cùng quyền thực hiện,
    // cập nhật trạng thái/lý do và lưu thay đổi.
    public TopicRegistrationResponse reject(Long registrationId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new AppException(ErrorCode.TOPIC_REGISTRATION_REJECTION_REASON_REQUIRED);
        }
        TopicRegistrationEntity registration = findRegistration(registrationId);
        requirePending(registration);
        registration.setStatus(TopicRegistrationStatusConstain.REJECTED);
        review(registration, reason.trim());
        return toResponse(registrationRepository.save(registration));
    }

    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Transactional
    // Hàm cancel: Nhận mã bản ghi của cancel, kiểm tra quyền và các quan hệ đang sử dụng, sau đó xóa hoặc chuyển bản
    // ghi sang trạng thái tương ứng.
    public TopicRegistrationResponse cancel(Long registrationId) {
        TopicRegistrationEntity registration = findRegistration(registrationId);
        if (!registration
                .getEnrollment()
                .getStudent()
                .getUserEntity()
                .getUserId()
                .equals(currentUserId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        graduationEnrollmentService.requireParticipationAllowed(
                registration.getEnrollment().getStudent().getIdStudent(),
                registration.getEnrollment().getDefensePeriod().getID_Defense());
        requirePending(registration);
        registration.setStatus(TopicRegistrationStatusConstain.CANCELLED);
        return toResponse(registrationRepository.save(registration));
    }

    // Hàm review: Nhận đăng ký đề tài và lý do; ghi thời điểm duyệt, người duyệt hiện tại và lý do từ chối vào bản ghi
    // đăng ký.
    private void review(TopicRegistrationEntity registration, String reason) {
        registration.setReviewedAt(LocalDateTime.now());
        registration.setReviewedBy(
                userRepository.findById(currentUserId()).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)));
        registration.setRejectionReason(reason);
    }

    // Hàm findRegistration: Nhận mã hoặc điều kiện tìm kiếm của findRegistration, truy vấn bản ghi/quan hệ tương ứng,
    // báo lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
    private TopicRegistrationEntity findRegistration(Long id) {
        return registrationRepository
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TOPIC_REGISTRATION_NOT_FOUND));
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requirePending(TopicRegistrationEntity registration) {
        if (registration.getStatus() != TopicRegistrationStatusConstain.PENDING) {
            throw new AppException(ErrorCode.TOPIC_REGISTRATION_OPERATION_NOT_ALLOWED);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireTeamWithoutTopic(TeamEntity team) {
        if (team.getTopic() != null) throw new AppException(ErrorCode.TEAM_ALREADY_HAS_TOPIC);
    }

    // Hàm currentStudent: Dùng userId hiện tại truy vấn UserEntity, kiểm tra tài khoản có hồ sơ sinh viên rồi trả về
    // StudentEntity cho các nghiệp vụ dành cho sinh viên.
    private StudentEntity currentStudent() {
        UserEntity user =
                userRepository.findById(currentUserId()).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getStudent() == null) throw new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND);
        return user.getStudent();
    }

    // Hàm currentUserId: Lấy tên định danh của tài khoản đã đăng nhập từ SecurityContext; dùng định danh này để truy
    // vấn hồ sơ và giới hạn dữ liệu theo người dùng hiện tại.
    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        return authentication.getName();
    }

    // Hàm normalize: Nhận ghi chú hoặc lý do đăng ký đề tài; chuyển chuỗi null/rỗng thành null và trim phần còn lại
    // trước khi lưu.
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // Hàm toResponse: Nhận TopicRegistrationEntity; lấy đề tài, nhóm, sinh viên, đợt bảo vệ và giảng viên ưu tiên rồi
    // ánh xạ đầy đủ sang DTO duyệt đăng ký.
    private TopicRegistrationResponse toResponse(TopicRegistrationEntity item) {
        StudentEntity student = item.getEnrollment().getStudent();
        LectureEntity lecturer = item.getPreferredSupervisor();
        return TopicRegistrationResponse.builder()
                .registrationId(item.getRegistrationId())
                .topicId(item.getTopic().getIdTopic())
                .topicTitle(item.getTopic().getTitle())
                .technology(item.getTopic().getTechnology())
                .teamId(item.getTeam() == null ? null : item.getTeam().getIdTeam())
                .teamName(item.getTeam() == null ? null : item.getTeam().getNameTeam())
                .studentCode(student.getStudentCode())
                .studentName(student.getFullNameStudent())
                .defensePeriodId(item.getEnrollment().getDefensePeriod().getID_Defense())
                .defensePeriodName(item.getEnrollment().getDefensePeriod().getPeriodName())
                .priority(item.getPriority())
                .status(item.getStatus())
                .preferredSupervisorId(lecturer == null ? null : lecturer.getLectureId())
                .preferredSupervisorName(lecturer == null ? null : lecturer.getFullNameLecture())
                .note(item.getNote())
                .rejectionReason(item.getRejectionReason())
                .submittedAt(item.getSubmittedAt())
                .reviewedAt(item.getReviewedAt())
                .build();
    }
}

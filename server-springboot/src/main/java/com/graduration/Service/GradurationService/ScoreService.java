package com.graduration.Service.GradurationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.CommitteeMemberRoleConstain;
import com.graduration.Constain.CommitteeMemberStatusConstain;
import com.graduration.Constain.ScoreStatusConstain;
import com.graduration.Constain.ScoreTypeConstain;
import com.graduration.DTO.Request.ScoreRequest;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.ScoreResponse;
import com.graduration.Repository.CommitteeMemberRepository;
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
    CommitteeMemberRepository committeeMemberRepository;
    StudentRepository studentRepository;
    TopicRepository topicRepository;
    ScoreMapper scoreMapper;
    GraduationEnrollmentService graduationEnrollmentService;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_REVIEWER')")
    @Transactional
    // Hàm saveDraft: Nhận dữ liệu đầu vào của saveDraft, kiểm tra các trường bắt buộc và quan hệ liên quan, tạo bản ghi
    // nghiệp vụ rồi lưu repository để trả kết quả cho API.
    public ScoreResponse saveDraft(String studentId, Long topicId, ScoreRequest request) {
        StudentEntity student =
                studentRepository.findById(studentId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        TopicEntity topic =
                topicRepository.findById(topicId).orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
        requireStudentTopic(student, topic);
        requireDefenseEligibility(student, topic);
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
        score.setComment(requireComment(request == null ? null : request.getComment()));
        return scoreMapper.toScoreResponse(scoreRepository.save(score));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    // Hàm getScore: Nhận mã hoặc điều kiện tìm kiếm của getScore, truy vấn bản ghi/quan hệ tương ứng, báo lỗi khi không
    // tồn tại và trả về dữ liệu đã ánh xạ.
    public ScoreResponse getScore(Long scoreId) {
        ScoreEntity score = findScore(scoreId);
        requireReadAccess(score);
        return scoreMapper.toScoreResponse(score);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_REVIEWER')")
    @Transactional(readOnly = true)
    // Tải bản ghi điểm hiện tại của một sinh viên trong đề tài để điền lại điểm/nhận xét khi mở modal chấm điểm.
    // Nếu chưa có bản ghi, trả về null để giao diện bắt đầu một phiếu chấm mới.
    public ScoreResponse getCurrentScore(String studentId, Long topicId) {
        StudentEntity student =
                studentRepository.findById(studentId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        TopicEntity topic =
                topicRepository.findById(topicId).orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
        requireStudentTopic(student, topic);
        requireDefenseEligibility(student, topic);
        requireScoringAccess(topic);
        LectureEntity evaluator = currentLecturer();
        return scoreRepository
                .findStudentTopicScore(studentId, topicId, evaluator.getLectureId(), ScoreTypeConstain.FINAL)
                .map(scoreMapper::toScoreResponse)
                .orElse(null);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_REVIEWER')")
    @Transactional
    // Hàm submit: Nhận scoreId; kiểm tra quyền chấm và trạng thái DRAFT, chuyển điểm sang SUBMITTED, ghi thời điểm gửi
    // rồi lưu kết quả.
    public ScoreResponse submit(Long scoreId) {
        ScoreEntity score = findScore(scoreId);
        requireDefenseEligibility(score.getStudent(), score.getTopic());
        requireScoringAccess(score.getTopic());
        requireStatus(score, ScoreStatusConstain.DRAFT);
        requireComment(score.getComment());
        score.setStatus(ScoreStatusConstain.SUBMITTED);
        score.setSubmittedAt(LocalDateTime.now());
        return scoreMapper.toScoreResponse(scoreRepository.save(score));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm publish: Nhận mã bản ghi và thông tin thao tác của publish, kiểm tra trạng thái hiện tại cùng quyền thực
    // hiện, cập nhật trạng thái/lý do và lưu thay đổi.
    public ScoreResponse publish(Long scoreId) {
        ScoreEntity score = findScore(scoreId);
        requireStatus(score, ScoreStatusConstain.SUBMITTED);
        score.setStatus(ScoreStatusConstain.LOCKED);
        score.setPublishedAt(LocalDateTime.now());
        return scoreMapper.toScoreResponse(scoreRepository.save(score));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_REVIEWER')")
    @Transactional
    // Mở khóa phiếu điểm đã gửi hoặc đã khóa sau khi kiểm tra quyền trên đề tài; xóa các mốc gửi/công bố và đưa về
    // DRAFT để Chủ tịch có thể sửa lại điểm cùng nhận xét.
    public ScoreResponse unlock(Long scoreId) {
        ScoreEntity score = findScore(scoreId);
        requireDefenseEligibility(score.getStudent(), score.getTopic());
        requireScoringAccess(score.getTopic());
        if (score.getStatus() != ScoreStatusConstain.SUBMITTED && score.getStatus() != ScoreStatusConstain.LOCKED) {
            throw new AppException(ErrorCode.SCORE_OPERATION_NOT_ALLOWED);
        }
        score.setStatus(ScoreStatusConstain.DRAFT);
        score.setSubmittedAt(null);
        score.setPublishedAt(null);
        return scoreMapper.toScoreResponse(scoreRepository.save(score));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_REVIEWER')")
    @Transactional
    // Xóa phiếu điểm nháp của sinh viên sau khi xác nhận quyền chấm và bảo đảm phiếu chưa được gửi/công bố.
    public void delete(Long scoreId) {
        ScoreEntity score = findScore(scoreId);
        requireDefenseEligibility(score.getStudent(), score.getTopic());
        requireScoringAccess(score.getTopic());
        requireStatus(score, ScoreStatusConstain.DRAFT);
        scoreRepository.delete(score);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    // Hàm getByDefensePeriod: Nhận các tham số lọc/phân trang của getByDefensePeriod, truy vấn dữ liệu phù hợp từ
    // repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public PageResponse<ScoreResponse> getByDefensePeriod(Long defensePeriodId, Integer page, Integer size) {
        return PageResponse.from(
                scoreRepository.findByDefensePeriod(defensePeriodId, PaginationSupport.pageRequest(page, size)),
                scoreMapper::toScoreResponse);
    }

    // Hàm applyScore: Nhận điểm từ request; kiểm tra điểm nằm trong khoảng cho phép, làm tròn hai chữ số thập phân rồi
    // gán vào ScoreEntity.
    private void applyScore(ScoreEntity score, ScoreRequest request) {
        if (request == null
                || request.getScore() == null
                || request.getScore().compareTo(MIN_SCORE) < 0
                || request.getScore().compareTo(MAX_SCORE) > 0) {
            throw new AppException(ErrorCode.SCORE_VALUE_INVALID);
        }
        score.setScore(request.getScore().setScale(2, java.math.RoundingMode.HALF_UP));
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireStudentTopic(StudentEntity student, TopicEntity topic) {
        if (topic.getTeam() == null
                || topic.getTeam().getStudentEntities().stream()
                        .noneMatch(member -> member.getIdStudent().equals(student.getIdStudent()))) {
            throw new AppException(ErrorCode.SCORE_STUDENT_TOPIC_MISMATCH);
        }
    }

    private void requireDefenseEligibility(StudentEntity student, TopicEntity topic) {
        if (topic.getDefensePeriod() != null) {
            graduationEnrollmentService.requireParticipationAllowed(
                    student.getIdStudent(), topic.getDefensePeriod().getID_Defense());
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireScoringAccess(TopicEntity topic) {
        if (isManager()) {
            return;
        }
        LectureEntity lecturer = currentLecturer();
        boolean chairperson = committeeMemberRepository.existsByTopicAndLecturerAndRoleAndStatus(
                topic.getIdTopic(),
                lecturer.getLectureId(),
                CommitteeMemberRoleConstain.CHAIRPERSON,
                CommitteeMemberStatusConstain.ACTIVE);
        if (!chairperson) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
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

    // Hàm findScore: Nhận mã hoặc điều kiện tìm kiếm của findScore, truy vấn bản ghi/quan hệ tương ứng, báo lỗi khi
    // không tồn tại và trả về dữ liệu đã ánh xạ.
    private ScoreEntity findScore(Long scoreId) {
        return scoreRepository
                .findWithRelationsById(scoreId)
                .orElseThrow(() -> new AppException(ErrorCode.SCORE_NOT_FOUND));
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireStatus(ScoreEntity score, ScoreStatusConstain expected) {
        if (score.getStatus() != expected) {
            throw new AppException(ErrorCode.SCORE_OPERATION_NOT_ALLOWED);
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

    // Hàm normalize: Nhận nhận xét chấm điểm từ ScoreRequest; chuyển giá trị trống thành null và trim nội dung trước
    // khi lưu ScoreEntity.
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // Hàm requireComment: Nhận nhận xét đánh giá đã chuẩn hóa; từ chối việc lưu/gửi điểm nếu nhận xét trống, bảo đảm
    // mỗi kết quả chấm điểm luôn có nội dung giải thích cho sinh viên.
    private String requireComment(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new AppException(ErrorCode.SCORE_COMMENT_NOT_BLANK);
        }
        return normalized;
    }
}

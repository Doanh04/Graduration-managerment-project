package com.graduration.Service.GradurationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.CommitteeMemberRoleConstain;
import com.graduration.Constain.CommitteeMemberStatusConstain;
import com.graduration.Constain.EnrollmentStatusConstain;
import com.graduration.Constain.ScoreStatusConstain;
import com.graduration.Constain.ScoreTypeConstain;
import com.graduration.DTO.Request.ScoreRequest;
import com.graduration.DTO.Request.CommitteeScoreSheetRequest;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.ScoreResponse;
import com.graduration.DTO.Response.CommitteeScoreEntryResponse;
import com.graduration.DTO.Response.CommitteeScoreSheetResponse;
import com.graduration.Repository.CommitteeMemberRepository;
import com.graduration.Repository.GraduationEnrollmentRepository;
import com.graduration.Repository.LectureRepository;
import com.graduration.Repository.ScoreRepository;
import com.graduration.Repository.StudentRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.entity.GraduationEnrollmentEntity;
import com.graduration.entity.ComitteesMemberEntity;
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
    GraduationEnrollmentRepository enrollmentRepository;
    ScoreMapper scoreMapper;
    GraduationEnrollmentService graduationEnrollmentService;
    com.graduration.Service.ManagerService.DefenseReportService defenseReportService;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_REVIEWER')")
    @Transactional
    // Hàm saveDraft: Nhận dữ liệu đầu vào của saveDraft, kiểm tra các trường bắt buộc và quan hệ liên quan, tạo bản ghi
    // nghiệp vụ rồi lưu repository để trả kết quả cho API.
    public ScoreResponse saveDraft(String studentId, Long topicId, ScoreRequest request) {
        StudentEntity student =
                studentRepository.findById(studentId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        TopicEntity topic =
                topicRepository.findById(topicId).orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
        requireOpenPeriod(topic);
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
        requireScoringAccess(topic);
        var automatic = scoreRepository.findByStudentAndDefensePeriod(studentId, topic.getDefensePeriod().getID_Defense())
                .stream().filter(score -> (score.getAutomaticScheduleId() != null
                                || score.getAutomaticReviewAssignmentId() != null
                                || score.getAutomaticFinalTopicId() != null)
                        && topicId.equals(score.getTopic().getIdTopic()))
                .findFirst();
        if (automatic.isPresent()) return scoreMapper.toScoreResponse(automatic.get());
        LectureEntity evaluator = currentLecturer();
        return scoreRepository
                .findStudentTopicScore(studentId, topicId, evaluator.getLectureId(), ScoreTypeConstain.FINAL)
                .map(scoreMapper::toScoreResponse)
                .orElse(null);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_REVIEWER')")
    @Transactional(readOnly = true)
    public CommitteeScoreSheetResponse getCommitteeScoreSheet(String studentId, Long topicId) {
        ScoringContext context = scoringContext(studentId, topicId, false);
        return buildCommitteeScoreSheet(context);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_REVIEWER')")
    @Transactional
    public CommitteeScoreSheetResponse saveCommitteeScoreSheet(
            String studentId, Long topicId, CommitteeScoreSheetRequest request) {
        ScoringContext context = scoringContext(studentId, topicId, true);
        if (request == null || request.getEntries() == null || request.getEntries().isEmpty()) {
            throw new AppException(ErrorCode.SCORE_VALUE_INVALID);
        }

        Map<String, ComitteesMemberEntity> membersByLecturer = new HashMap<>();
        context.members().forEach(member -> membersByLecturer.put(member.getLecture().getLectureId(), member));
        Map<String, ScoreEntity> scoresByLecturer = new HashMap<>();
        context.scores().forEach(score -> scoresByLecturer.put(score.getLecture().getLectureId(), score));
        var requestedLecturers = new HashSet<String>();
        var changed = new ArrayList<ScoreEntity>();

        for (var entry : request.getEntries()) {
            String lecturerId = entry.getLecturerId() == null ? "" : entry.getLecturerId().trim();
            if (!requestedLecturers.add(lecturerId)) {
                throw new AppException(ErrorCode.SCORE_OPERATION_NOT_ALLOWED);
            }
            ComitteesMemberEntity member = membersByLecturer.get(lecturerId);
            if (member == null) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
            ScoreEntity score = scoresByLecturer.get(lecturerId);
            if (score != null && score.getStatus() != ScoreStatusConstain.DRAFT) {
                boolean unchanged = score.getScore() != null
                        && entry.getScore() != null
                        && score.getScore().compareTo(entry.getScore()) == 0
                        && java.util.Objects.equals(normalize(score.getComment()), normalize(entry.getComment()));
                if (unchanged) continue;
                throw new AppException(ErrorCode.SCORE_OPERATION_NOT_ALLOWED);
            }
            if (score == null) {
                score = ScoreEntity.builder()
                        .student(context.student())
                        .topic(context.topic())
                        .lecture(member.getLecture())
                        .scoreType(ScoreTypeConstain.FINAL)
                        .status(ScoreStatusConstain.DRAFT)
                        .build();
                scoresByLecturer.put(lecturerId, score);
            }
            applyScore(score, ScoreRequest.builder().score(entry.getScore()).comment(entry.getComment()).build());
            score.setComment(requireComment(entry.getComment()));
            score.setSubmittedAt(null);
            score.setPublishedAt(null);
            changed.add(score);
        }
        scoreRepository.saveAll(changed);
        return buildCommitteeScoreSheet(new ScoringContext(
                context.student(), context.topic(), context.members(), new ArrayList<>(scoresByLecturer.values())));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_REVIEWER')")
    @Transactional
    public CommitteeScoreSheetResponse submitCommitteeScoreSheet(String studentId, Long topicId) {
        ScoringContext context = scoringContext(studentId, topicId, true);
        Map<String, ScoreEntity> scoresByLecturer = new HashMap<>();
        context.scores().forEach(score -> scoresByLecturer.put(score.getLecture().getLectureId(), score));
        for (ComitteesMemberEntity member : context.members()) {
            ScoreEntity score = scoresByLecturer.get(member.getLecture().getLectureId());
            if (score == null || score.getScore() == null) {
                throw new AppException(ErrorCode.SCORE_VALUE_INVALID);
            }
            requireComment(score.getComment());
        }

        LocalDateTime submittedAt = LocalDateTime.now();
        boolean correctionAfterPublication = context.scores().stream()
                .anyMatch(score -> score.getPublishedAt() != null);
        context.scores().forEach(score -> {
            score.setStatus(ScoreStatusConstain.LOCKED);
            score.setSubmittedAt(submittedAt);
            score.setPublishedAt(correctionAfterPublication ? submittedAt : null);
        });
        scoreRepository.saveAll(context.scores());
        return buildCommitteeScoreSheet(context);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_REVIEWER')")
    @Transactional
    // Gửi điểm đồng thời công bố kết quả cho sinh viên sau khi Thư ký hoàn tất chấm bảo vệ.
    public ScoreResponse submit(Long scoreId) {
        ScoreEntity score = findScore(scoreId);
        requireOpenPeriod(score.getTopic());
        requireDefenseEligibility(score.getStudent(), score.getTopic());
        requireScoringAccess(score.getTopic());
        requireStatus(score, ScoreStatusConstain.DRAFT);
        requireComment(score.getComment());
        LocalDateTime submittedAt = LocalDateTime.now();
        score.setStatus(ScoreStatusConstain.LOCKED);
        score.setSubmittedAt(submittedAt);
        score.setPublishedAt(null);
        return scoreMapper.toScoreResponse(scoreRepository.save(score));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm publish: Nhận mã bản ghi và thông tin thao tác của publish, kiểm tra trạng thái hiện tại cùng quyền thực
    // hiện, cập nhật trạng thái/lý do và lưu thay đổi.
    public ScoreResponse publish(Long scoreId) {
        ScoreEntity score = findScore(scoreId);
        defenseReportService.requireEligible(score.getTopic().getDefenseSchedule());
        requireOpenPeriod(score.getTopic());
        requireStatus(score, ScoreStatusConstain.SUBMITTED);
        score.setStatus(ScoreStatusConstain.LOCKED);
        score.setPublishedAt(LocalDateTime.now());
        return scoreMapper.toScoreResponse(scoreRepository.save(score));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_REVIEWER')")
    @Transactional
    // Mở khóa phiếu điểm đã gửi hoặc đã khóa sau khi kiểm tra quyền trên đề tài; xóa các mốc gửi/công bố và đưa về
    // DRAFT để Thư ký có thể sửa lại điểm cùng nhận xét.
    public ScoreResponse unlock(Long scoreId) {
        ScoreEntity score = findScore(scoreId);
        requireOpenPeriod(score.getTopic());
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
        requireOpenPeriod(score.getTopic());
        requireDefenseEligibility(score.getStudent(), score.getTopic());
        requireScoringAccess(score.getTopic());
        requireStatus(score, ScoreStatusConstain.DRAFT);
        scoreRepository.delete(score);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_STUDENT')")
    @Transactional(readOnly = true)
    // Hàm getByDefensePeriod: Nhận các tham số lọc/phân trang của getByDefensePeriod, truy vấn dữ liệu phù hợp từ
    // repository, ánh xạ từng entity sang DTO và trả về cho giao diện.
    public PageResponse<ScoreResponse> getByDefensePeriod(Long defensePeriodId, Integer page, Integer size) {
        if (SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_STUDENT".equals(authority.getAuthority()))) {
            StudentEntity student = studentRepository
                    .findByUserEntity_UserId(SecurityContextHolder.getContext()
                            .getAuthentication()
                            .getName())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            return PageResponse.of(
                    scoreRepository.findByStudentAndDefensePeriod(student.getIdStudent(), defensePeriodId).stream()
                            .filter(score -> score.getPublishedAt() != null)
                            .map(scoreMapper::toScoreResponse)
                            .toList());
        }
        return PageResponse.from(
                scoreRepository.findByDefensePeriod(defensePeriodId, PaginationSupport.pageRequest(page, size)),
                scoreMapper::toScoreResponse);
    }

    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Transactional(readOnly = true)
    public PageResponse<ScoreResponse> getMyPublishedScores() {
        StudentEntity student = studentRepository
                .findByUserEntity_UserId(
                        SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        List<ScoreResponse> results = new ArrayList<>(publishedResultsForStudent(student));

        enrollmentRepository
                .findByStudent_UserEntity_UserIdOrderByEnrolledAtDesc(
                        SecurityContextHolder.getContext().getAuthentication().getName())
                .stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatusConstain.WITHDRAWN)
                .filter(enrollment -> results.stream()
                        .noneMatch(result ->
                                enrollment.getDefensePeriod().getID_Defense().equals(result.getDefensePeriodId())))
                .map(enrollment -> withdrawnResult(student, enrollment))
                .forEach(results::add);

        results.sort(
                Comparator.comparing(ScoreResponse::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ScoreResponse::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return PageResponse.of(results);
    }

    /** Chuẩn hóa điểm đã gửi trong dữ liệu cũ thành trạng thái đã công bố khi sinh viên xem kết quả. */
    private ScoreResponse normalizeStudentVisibleStatus(ScoreResponse score) {
        if (score.getStatus() == ScoreStatusConstain.SUBMITTED) {
            score.setStatus(ScoreStatusConstain.LOCKED);
            if (score.getPublishedAt() == null) {
                score.setPublishedAt(score.getSubmittedAt());
            }
        }
        return score;
    }

    private List<ScoreResponse> publishedResultsForStudent(StudentEntity student) {
        Map<Long, List<ScoreEntity>> byTopic = new LinkedHashMap<>();
        scoreRepository.findPublishedByStudent(student.getIdStudent()).forEach(score ->
                byTopic.computeIfAbsent(score.getTopic().getIdTopic(), ignored -> new ArrayList<>()).add(score));
        List<ScoreResponse> results = new ArrayList<>();
        byTopic.values().forEach(publishedRows -> {
            ScoreEntity automatic = publishedRows.stream()
                    .filter(score -> score.getAutomaticScheduleId() != null
                            || score.getAutomaticReviewAssignmentId() != null
                            || score.getAutomaticFinalTopicId() != null)
                    .findFirst()
                    .orElse(null);
            if (automatic != null) {
                results.add(normalizeStudentVisibleStatus(scoreMapper.toScoreResponse(automatic)));
                return;
            }

            TopicEntity topic = publishedRows.get(0).getTopic();
            Map<String, CommitteeMemberRoleConstain> roles = committeeRoles(topic);
            List<ScoreEntity> allRows = scoreRepository.findCommitteeScores(
                            student.getIdStudent(), topic.getIdTopic())
                    .stream()
                    .filter(score -> score.getLecture() != null
                            && roles.containsKey(score.getLecture().getLectureId()))
                    .toList();
            if (allRows.isEmpty()
                    || roles.isEmpty()
                    || allRows.size() != roles.size()
                    || allRows.stream().anyMatch(score -> score.getStatus() != ScoreStatusConstain.LOCKED
                            || score.getPublishedAt() == null)) {
                return;
            }
            ScoreResponse aggregate = scoreMapper.toScoreResponse(allRows.get(0));
            aggregate.setTotalScore(allRows.stream()
                    .map(ScoreEntity::getScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(allRows.size()), 2, RoundingMode.HALF_UP));
            aggregate.setStatus(ScoreStatusConstain.LOCKED);
            aggregate.setComment("Điểm trung bình của " + allRows.size() + " thành viên hội đồng");
            aggregate.setLecturerId(null);
            aggregate.setLecturerCode(null);
            aggregate.setLecturerName(null);
            aggregate.setCommitteeScores(allRows.stream()
                    .map(score -> CommitteeScoreEntryResponse.builder()
                            .scoreId(score.getId())
                            .lecturerId(score.getLecture().getLectureId())
                            .lecturerCode(score.getLecture().getLectureCode())
                            .lecturerName(score.getLecture().getFullNameLecture())
                            .role(roles.get(score.getLecture().getLectureId()))
                            .score(score.getScore())
                            .comment(score.getComment())
                            .status(score.getStatus())
                            .updatedAt(score.getUpdatedAt())
                            .submittedAt(score.getSubmittedAt())
                            .build())
                    .toList());
            aggregate.setPublishedAt(allRows.stream()
                    .map(ScoreEntity::getPublishedAt)
                    .filter(java.util.Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null));
            results.add(aggregate);
        });
        return results;
    }

    private Map<String, CommitteeMemberRoleConstain> committeeRoles(TopicEntity topic) {
        if (topic.getDefenseSchedule() == null || topic.getDefenseSchedule().getDefenseCommittees() == null) {
            return Map.of();
        }
        Map<String, CommitteeMemberRoleConstain> roles = new HashMap<>();
        committeeMemberRepository.findByDefenseCommittees_IdComitteesAndStatusOrderByRoleAsc(
                        topic.getDefenseSchedule().getDefenseCommittees().getIdComittees(),
                        CommitteeMemberStatusConstain.ACTIVE)
                .stream()
                .filter(member -> contributesToFinalScore(member.getRole()))
                .forEach(member -> roles.put(member.getLecture().getLectureId(), member.getRole()));
        return roles;
    }

    /**
     * Sinh viên bị hủy tư cách phải luôn nhìn thấy kết quả 0, kể cả khi hội đồng chưa tạo phiếu điểm.
     * Đây là kết quả nghiệp vụ phát sinh từ hồ sơ ghi danh nên không cần một giảng viên giả để thỏa khóa ngoại của score.
     */
    private ScoreResponse withdrawnResult(StudentEntity student, GraduationEnrollmentEntity enrollment) {
        TopicEntity topic = topicRepository
                .findByStudentAndDefensePeriod(
                        student.getIdStudent(), enrollment.getDefensePeriod().getID_Defense())
                .orElse(null);
        return ScoreResponse.builder()
                .totalScore(BigDecimal.ZERO.setScale(2))
                .scoreType(ScoreTypeConstain.FINAL)
                .comment("Sinh viên bị hủy tư cách thi")
                .status(ScoreStatusConstain.LOCKED)
                .studentId(student.getIdStudent())
                .studentCode(student.getStudentCode())
                .studentName(student.getFullNameStudent())
                .teamId(
                        topic == null || topic.getTeam() == null
                                ? null
                                : topic.getTeam().getIdTeam())
                .teamName(
                        topic == null || topic.getTeam() == null
                                ? null
                                : topic.getTeam().getNameTeam())
                .topicId(topic == null ? null : topic.getIdTopic())
                .topicTitle(topic == null ? "Bị cấm bảo vệ" : topic.getTitle())
                .defensePeriodId(enrollment.getDefensePeriod().getID_Defense())
                .defensePeriodName(enrollment.getDefensePeriod().getPeriodName())
                .academicYear(enrollment.getDefensePeriod().getAcademicYear() == null
                        ? null
                        : enrollment.getDefensePeriod().getAcademicYear().getAcademicYear())
                .updatedAt(enrollment.getCompletedAt())
                .publishedAt(enrollment.getCompletedAt())
                .build();
    }

    // Hàm applyScore: Nhận điểm từ request; kiểm tra điểm nằm trong khoảng cho phép, làm tròn hai chữ số thập phân rồi
    // gán vào ScoreEntity.
    private ScoringContext scoringContext(String studentId, Long topicId, boolean requireWriteConditions) {
        StudentEntity student =
                studentRepository.findById(studentId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        TopicEntity topic =
                topicRepository.findById(topicId).orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
        requireStudentTopic(student, topic);
        requireScoringAccess(topic);
        if (requireWriteConditions) {
            requireOpenPeriod(topic);
            requireDefenseEligibility(student, topic);
        }
        if (topic.getDefenseSchedule() == null || topic.getDefenseSchedule().getDefenseCommittees() == null) {
            throw new AppException(ErrorCode.DEFENSE_SCHEDULE_NOT_FOUND);
        }
        Long committeeId = topic.getDefenseSchedule().getDefenseCommittees().getIdComittees();
        List<ComitteesMemberEntity> members = committeeMemberRepository
                .findByDefenseCommittees_IdComitteesAndStatusOrderByRoleAsc(
                        committeeId, CommitteeMemberStatusConstain.ACTIVE)
                .stream()
                .filter(member -> contributesToFinalScore(member.getRole()))
                .toList();
        if (members.isEmpty()) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_NOT_READY);
        }
        var activeLecturerIds = members.stream()
                .map(member -> member.getLecture().getLectureId())
                .collect(java.util.stream.Collectors.toSet());
        List<ScoreEntity> committeeScores = scoreRepository.findCommitteeScores(studentId, topicId).stream()
                .filter(score -> score.getLecture() != null
                        && activeLecturerIds.contains(score.getLecture().getLectureId()))
                .toList();
        return new ScoringContext(student, topic, members, committeeScores);
    }

    private CommitteeScoreSheetResponse buildCommitteeScoreSheet(ScoringContext context) {
        Map<String, ScoreEntity> scoresByLecturer = new HashMap<>();
        context.scores().forEach(score -> scoresByLecturer.put(score.getLecture().getLectureId(), score));
        List<CommitteeScoreEntryResponse> entries = context.members().stream().map(member -> {
            ScoreEntity score = scoresByLecturer.get(member.getLecture().getLectureId());
            return CommitteeScoreEntryResponse.builder()
                    .scoreId(score == null ? null : score.getId())
                    .lecturerId(member.getLecture().getLectureId())
                    .lecturerCode(member.getLecture().getLectureCode())
                    .lecturerName(member.getLecture().getFullNameLecture())
                    .role(member.getRole())
                    .score(score == null ? null : score.getScore())
                    .comment(score == null ? null : score.getComment())
                    .status(score == null ? ScoreStatusConstain.DRAFT : score.getStatus())
                    .updatedAt(score == null ? null : score.getUpdatedAt())
                    .submittedAt(score == null ? null : score.getSubmittedAt())
                    .build();
        }).toList();
        List<ScoreEntity> scored = context.members().stream()
                .map(member -> scoresByLecturer.get(member.getLecture().getLectureId()))
                .filter(score -> score != null && score.getScore() != null)
                .toList();
        BigDecimal average = scored.isEmpty()
                ? null
                : scored.stream()
                        .map(ScoreEntity::getScore)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(scored.size()), 2, RoundingMode.HALF_UP);
        boolean complete = scored.size() == context.members().size()
                && scored.stream().allMatch(score -> normalize(score.getComment()) != null);
        boolean published = complete
                && scored.stream().allMatch(score -> score.getStatus() == ScoreStatusConstain.LOCKED
                        && score.getPublishedAt() != null);
        var schedule = context.topic().getDefenseSchedule();
        return CommitteeScoreSheetResponse.builder()
                .studentId(context.student().getIdStudent())
                .studentCode(context.student().getStudentCode())
                .studentName(context.student().getFullNameStudent())
                .topicId(context.topic().getIdTopic())
                .topicTitle(context.topic().getTitle())
                .committeeId(schedule.getDefenseCommittees().getIdComittees())
                .committeeName(schedule.getDefenseCommittees().getComitteesName())
                .finalScore(average)
                .scoredMembers(scored.size())
                .totalMembers(context.members().size())
                .complete(complete)
                .published(published)
                .entries(entries)
                .build();
    }

    private record ScoringContext(
            StudentEntity student,
            TopicEntity topic,
            List<ComitteesMemberEntity> members,
            List<ScoreEntity> scores) {}

    private static boolean contributesToFinalScore(CommitteeMemberRoleConstain role) {
        return role == CommitteeMemberRoleConstain.CHAIRPERSON
                || role == CommitteeMemberRoleConstain.REVIEWER;
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

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireStudentTopic(StudentEntity student, TopicEntity topic) {
        if (topic.getTeam() == null
                || topic.getTeam().getStudentEntities().stream()
                        .noneMatch(member -> member.getIdStudent().equals(student.getIdStudent()))) {
            throw new AppException(ErrorCode.SCORE_STUDENT_TOPIC_MISMATCH);
        }
    }

    private void requireOpenPeriod(TopicEntity topic) {
        if (topic == null || topic.getDefensePeriod() == null) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND);
        }
        if (topic.getDefensePeriod().isFinished()) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_FINISHED);
        }
    }

    private void requireDefenseEligibility(StudentEntity student, TopicEntity topic) {
        defenseReportService.requireEligible(topic.getDefenseSchedule());
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
        boolean secretary = committeeMemberRepository.existsByTopicAndLecturerAndRoleAndStatus(
                topic.getIdTopic(),
                lecturer.getLectureId(),
                CommitteeMemberRoleConstain.SECRETARY,
                CommitteeMemberStatusConstain.ACTIVE);
        if (!secretary) {
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
                && score.getStatus() == ScoreStatusConstain.LOCKED
                && score.getPublishedAt() != null;
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

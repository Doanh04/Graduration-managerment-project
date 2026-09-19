package com.graduration.Service.GradurationService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.CategoryTopicConstain;
import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.Constain.SupervisorAssignmentStatusConstain;
import com.graduration.Constain.SupervisorRoleConstain;
import com.graduration.Constain.TopicRegistrationStatusConstain;
import com.graduration.Constain.TopicStatusConstain;
import com.graduration.DTO.Request.CreateSupervisorTopicProposalRequest;
import com.graduration.DTO.Request.CreateTopicRequest;
import com.graduration.DTO.Request.UpdateTopicRequest;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.TeamResponse;
import com.graduration.DTO.Response.TopicResponse;
import com.graduration.Repository.DefensePeriodRepository;
import com.graduration.Repository.LectureRepository;
import com.graduration.Repository.StudentRepository;
import com.graduration.Repository.TeamRepository;
import com.graduration.Repository.TopicRegistrationRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.Repository.TopicSupervisorRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.GraduationEnrollmentEntity;
import com.graduration.entity.LectureEntity;
import com.graduration.entity.StudentEntity;
import com.graduration.entity.TeamEntity;
import com.graduration.entity.TopicEntity;
import com.graduration.entity.TopicRegistrationEntity;
import com.graduration.entity.TopicSuperVisorEntity;
import com.graduration.entity.UserEntity;
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
    private static final long MAX_TOPIC_FILE_SIZE = 10 * 1024 * 1024L;
    private static final Set<String> ALLOWED_TOPIC_FILE_EXTENSIONS = Set.of("pdf", "doc", "docx");

    TopicRepository topicRepository;
    TopicRegistrationRepository topicRegistrationRepository;
    DefensePeriodRepository defensePeriodRepository;
    UserRepository userRepository;
    TeamRepository teamRepository;
    TopicMapper topicMapper;
    StudentRepository studentRepository;
    LectureRepository lectureRepository;
    TopicSupervisorRepository topicSupervisorRepository;
    GraduationEnrollmentService graduationEnrollmentService;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_STUDENT')")
    @Transactional
    // Hàm createTopic: Nhận dữ liệu đầu vào của createTopic, kiểm tra các trường bắt buộc và quan hệ liên quan, tạo bản
    // ghi nghiệp vụ rồi lưu repository để trả kết quả cho API.
    public TopicResponse createTopic(CreateTopicRequest request) {
        return createTopicInternal(request);
    }

    /**
     * Nhận đề xuất của giảng viên kèm tên nhóm và mã các sinh viên; kiểm tra đợt
     * bảo vệ, tiêu đề, trạng thái sinh viên rồi tạo nhóm tạm và đề tài chờ admin duyệt.
     */
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    @Transactional
    public TopicResponse createSupervisorProposal(CreateSupervisorTopicProposalRequest request) {
        return createSupervisorProposal(request, null);
    }

    /** Nhận đề xuất kèm tệp mô tả tùy chọn; lưu tệp PDF/DOC/DOCX vào cột BLOB của đề tài cùng giao dịch tạo nhóm. */
    @PreAuthorize("hasAuthority('ROLE_SUPERVISOR')")
    @Transactional
    public TopicResponse createSupervisorProposal(CreateSupervisorTopicProposalRequest request, MultipartFile file) {
        validateTopicFile(file);
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        validateRequest(request.getTitle(), CategoryTopicConstain.LECTURER);
        if (request.getTeamName() == null || request.getTeamName().isBlank()) {
            throw new AppException(ErrorCode.TEAM_NAME_NOT_BLANK);
        }
        if (request.getStudentCodes() == null || request.getStudentCodes().isEmpty()) {
            throw new AppException(ErrorCode.STUDENT_NOT_BLANK);
        }

        DefensePeriodEntity period = findActiveDefensePeriod(request.getDefensePeriodId());
        String title = request.getTitle().trim();
        if (topicRepository.existsTitleInDefensePeriod(title, period.getID_Defense())) {
            throw new AppException(ErrorCode.TOPIC_ALREADY_EXISTS);
        }
        String teamName = request.getTeamName().trim();
        if (teamRepository.existsByNameAndDefensePeriod(teamName, period.getID_Defense())) {
            throw new AppException(ErrorCode.TEAM_ALREADY_EXISTS);
        }

        Set<String> codes = new LinkedHashSet<>();
        request.getStudentCodes().forEach(code -> {
            if (code != null && !code.isBlank()) codes.add(code.trim());
        });
        if (codes.isEmpty()) {
            throw new AppException(ErrorCode.STUDENT_NOT_BLANK);
        }
        List<StudentEntity> students = studentRepository.findAllByStudentCodeIn(codes);
        if (students.size() != codes.size()) {
            throw new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND);
        }
        if (students.stream()
                .anyMatch(student -> teamRepository.existsStudentInAnotherTeamOfDefensePeriod(
                        student.getIdStudent(), period.getID_Defense(), -1L))) {
            throw new AppException(ErrorCode.STUDENT_ALREADY_IN_TEAM);
        }

        TeamEntity team = TeamEntity.builder()
                .nameTeam(teamName)
                .description("Nhóm đề xuất bởi giảng viên")
                .joinDate(java.time.LocalDate.now())
                .defensePeriod(period)
                .studentEntities(new ArrayList<>())
                .build();
        team = teamRepository.save(team);
        for (StudentEntity student : students) {
            team.getStudentEntities().add(student);
            student.getTeamMemberships().add(team);
        }
        studentRepository.saveAll(students);

        TopicEntity topic = TopicEntity.builder()
                .title(title)
                .description(normalize(request.getDescription()))
                .objective(normalize(request.getObjective()))
                .technology(normalize(request.getTechnology()))
                .categoryTopic(CategoryTopicConstain.LECTURER)
                .status(TopicStatusConstain.PENDING_APPROVAL)
                .createdBy(currentUserId())
                .defensePeriod(period)
                .build();
        if (file != null && !file.isEmpty()) {
            topic.setFileData(encodeTopicFile(file));
        }
        TopicEntity savedTopic = topicRepository.save(topic);
        createProposalRegistration(savedTopic, team, students, period);
        return toResponse(savedTopic);
    }

    /**
     * Lưu một bản ghi đăng ký nội bộ đại diện cho cả nhóm đề xuất.  Đề xuất của
     * giảng viên là một yêu cầu ở cấp nhóm, không phải mỗi sinh viên gửi một
     * yêu cầu riêng; tạo nhiều bản ghi sẽ làm hàng đợi duyệt bị trùng.
     */
    private void createProposalRegistration(
            TopicEntity topic, TeamEntity team, List<StudentEntity> students, DefensePeriodEntity period) {
        StudentEntity representative = students.get(0);
        GraduationEnrollmentEntity enrollment = graduationEnrollmentService.ensureEligible(
                representative, period, "Chờ duyệt đề xuất đề tài của giảng viên");
        topicRegistrationRepository.save(TopicRegistrationEntity.builder()
                .enrollment(enrollment)
                .topic(topic)
                .team(team)
                .priority(1)
                .status(TopicRegistrationStatusConstain.PENDING)
                .note("Đề xuất đề tài của giảng viên")
                .build());
    }

    /** Nhận dữ liệu đề tài dạng multipart, tạo đề tài theo luồng cũ rồi lưu tệp mô tả tùy chọn vào cột BLOB của đề tài. */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_STUDENT')")
    @Transactional
    public TopicResponse createTopic(CreateTopicRequest request, MultipartFile file) {
        validateTopicFile(file);
        TopicResponse response = createTopicInternal(request);
        if (file == null || file.isEmpty()) {
            return response;
        }
        TopicEntity topic = findTopic(response.getTopicId());
        topic.setFileData(encodeTopicFile(file));
        topicRepository.save(topic);
        return toResponse(topic);
    }

    // Hàm createTopicInternal: Nhận dữ liệu đầu vào của createTopicInternal, kiểm tra các trường bắt buộc và quan hệ
    // liên quan, tạo bản ghi nghiệp vụ rồi lưu repository để trả kết quả cho API.
    private TopicResponse createTopicInternal(CreateTopicRequest request) {
        TeamEntity proposingTeam = null;
        if (hasAuthority("ROLE_STUDENT")) {
            proposingTeam = requireStudentTeamWithoutTopic();
            if (request == null || request.getCategoryTopic() != CategoryTopicConstain.STUDENT) {
                throw new AppException(ErrorCode.TOPIC_CATEGORY_NOT_BLANK);
            }
            if (topicRepository.findStudentProposalsByTeam(proposingTeam.getIdTeam()).stream()
                    .anyMatch(topic -> topic.getStatus() == TopicStatusConstain.DRAFT
                            || topic.getStatus() == TopicStatusConstain.PENDING_APPROVAL)) {
                throw new AppException(ErrorCode.TOPIC_REGISTRATION_ALREADY_PENDING);
            }
        }
        validateRequest(
                request == null ? null : request.getTitle(), request == null ? null : request.getCategoryTopic());
        DefensePeriodEntity period = findActiveDefensePeriod(request.getDefensePeriodId());
        if (proposingTeam != null) {
            StudentEntity currentStudent = studentRepository
                    .findByUserEntity_UserId(currentUserId())
                    .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));
            graduationEnrollmentService.requireParticipationAllowed(
                    currentStudent.getIdStudent(), period.getID_Defense());
        }
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
        return toResponse(topicRepository.save(topic));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_STUDENT')")
    // Hàm importTopics: Nhận tệp hoặc dòng dữ liệu đầu vào của importTopics, đọc các ô, kiểm tra định dạng và lỗi
    // nghiệp vụ rồi tạo danh sách dữ liệu hợp lệ để lưu.
    public ImportTopicResult importTopics(MultipartFile file) {
        if (file == null
                || file.isEmpty()
                || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new AppException(ErrorCode.INVALID_EXCEL_FILE);
        }

        List<TopicResponse> importedTopics = new ArrayList<>();
        List<ImportTopicError> errors = new ArrayList<>();
        int totalRows = 0;
        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            validateImportHeader(sheet.getRow(0), formatter);
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null || isBlankRow(row, formatter)) {
                    continue;
                }
                totalRows++;
                String title = cellValue(row, 0, formatter);
                try {
                    CreateTopicRequest request = CreateTopicRequest.builder()
                            .title(title)
                            .description(cellValue(row, 1, formatter))
                            .objective(cellValue(row, 2, formatter))
                            .technology(cellValue(row, 3, formatter))
                            .categoryTopic(parseCategory(cellValue(row, 4, formatter)))
                            .defensePeriodId(parseDefensePeriodId(cellValue(row, 5, formatter)))
                            .build();
                    importedTopics.add(createTopicInternal(request));
                } catch (RuntimeException exception) {
                    errors.add(new ImportTopicError(index + 1, title, exception.getMessage()));
                }
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw new AppException(ErrorCode.INVALID_EXCEL_FILE);
        }
        if (totalRows == 0) {
            throw new AppException(ErrorCode.INVALID_EXCEL_FILE);
        }
        return new ImportTopicResult(totalRows, importedTopics.size(), errors.size(), importedTopics, errors);
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void validateImportHeader(Row header, DataFormatter formatter) {
        String[] expected = {"title", "description", "objective", "technology", "categoryTopic", "defensePeriodId"};
        if (header == null) {
            throw new AppException(ErrorCode.INVALID_EXCEL_FILE);
        }
        for (int column = 0; column < expected.length; column++) {
            if (!expected[column].equalsIgnoreCase(cellValue(header, column, formatter))) {
                throw new AppException(ErrorCode.INVALID_EXCEL_FILE);
            }
        }
    }

    // Hàm parseCategory: Nhận chuỗi phân loại đề tài từ request/import; trim và chuyển về giá trị enum hợp lệ, nếu
    // không khớp thì phát sinh lỗi dữ liệu đầu vào.
    private CategoryTopicConstain parseCategory(String value) {
        String original = value == null ? "" : value.trim();
        if (original.isBlank()) {
            throw new IllegalArgumentException(
                    "Cột categoryTopic (Nguồn đề xuất) đang để trống. Hãy nhập LECTURER/Giảng viên hoặc STUDENT/Sinh viên.");
        }

        String normalized = Normalizer.normalize(original, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("[_-]+", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);

        if (normalized.equals("LECTURER") || normalized.equals("GV") || normalized.contains("GIANG VIEN")) {
            return CategoryTopicConstain.LECTURER;
        }
        if (normalized.equals("STUDENT") || normalized.equals("SV") || normalized.contains("SINH VIEN")) {
            return CategoryTopicConstain.STUDENT;
        }
        throw new IllegalArgumentException("Nguồn đề xuất \"" + original
                + "\" không hợp lệ. Hãy dùng LECTURER/Giảng viên hoặc STUDENT/Sinh viên.");
    }

    // Hàm parseDefensePeriodId: Nhận giá trị mã đợt bảo vệ từ ô Excel; chuyển chuỗi số thành Long và báo lỗi khi ô
    // thiếu hoặc không đúng định dạng.
    private Long parseDefensePeriodId(String value) {
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND);
        }
    }

    // Hàm cellValue: Nhận ô Excel của dòng import đề tài; đọc giá trị chuỗi/số/ngày theo kiểu ô và chuyển thành chuỗi
    // để tạo CreateTopicRequest.
    private String cellValue(Row row, int column, DataFormatter formatter) {
        return formatter.formatCellValue(row.getCell(column)).trim();
    }

    // Hàm isBlankRow: Nhận dòng dữ liệu Excel; xác định dòng không có bất kỳ giá trị hữu ích nào trước khi thực hiện
    // kiểm tra và import.
    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (int column = 0; column < 6; column++) {
            if (!cellValue(row, column, formatter).isBlank()) {
                return false;
            }
        }
        return true;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    // Hàm getTopic: Nhận mã hoặc điều kiện tìm kiếm của getTopic, truy vấn bản ghi/quan hệ tương ứng, báo lỗi khi không
    // tồn tại và trả về dữ liệu đã ánh xạ.
    public TopicResponse getTopic(Long topicId) {
        return toResponse(findTopic(topicId));
    }

    /** Tải tệp PDF/DOC/DOCX của đề tài từ cột BLOB sau khi kiểm tra đề tài và quyền truy cập. */
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public DownloadedTopic downloadTopicFile(Long topicId) {
        TopicEntity topic = findTopic(topicId);
        TopicFile file =
                decodeTopicFile(topic.getFileData()).orElseThrow(() -> new AppException(ErrorCode.FILE_STORAGE_ERROR));
        return new DownloadedTopic(new ByteArrayResource(file.content()), file.fileName(), file.contentType());
    }

    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT', 'ROLE_SUPERVISOR')")
    @Transactional
    // Hàm getMyProposals: Nhận các tham số lọc/phân trang của getMyProposals, truy vấn dữ liệu phù hợp từ repository,
    // ánh xạ từng entity sang DTO và trả về cho giao diện.
    public List<TopicResponse> getMyProposals() {
        // Giảng viên hướng dẫn theo dõi các đề tài do chính mình đề xuất;
        // sinh viên vẫn dùng luồng cũ để xem các đề xuất của nhóm mình.
        if (hasAuthority("ROLE_SUPERVISOR")) {
            return topicRepository.findByCreatedByOrderByCreatedAtDesc(currentUserId()).stream()
                    .map(this::toResponse)
                    .toList();
        }
        TeamEntity team = teamRepository
                .findByStudentEntities_UserEntity_UserId(currentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_TEAM_REQUIRED));
        return topicRepository.findStudentProposalsByTeam(team.getIdTeam()).stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    // Hàm getTopics: Nhận mã hoặc điều kiện tìm kiếm của getTopics, truy vấn bản ghi/quan hệ tương ứng, báo lỗi khi
    // không tồn tại và trả về dữ liệu đã ánh xạ.
    public PageResponse<TopicResponse> getTopics(
            Integer page,
            Integer size,
            Integer academicYearId,
            Long defensePeriodId,
            CategoryTopicConstain categoryTopic,
            TopicStatusConstain status,
            String keyword,
            boolean excludeStudentProposals) {
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
        if (excludeStudentProposals) {
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.notEqual(root.get("categoryTopic"), CategoryTopicConstain.STUDENT),
                    cb.not(root.get("status")
                            .in(
                                    TopicStatusConstain.DRAFT,
                                    TopicStatusConstain.PENDING_APPROVAL,
                                    TopicStatusConstain.REJECTED))));
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
                this::toResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    /**
     * Lấy các đề tài thuộc đúng đợt bảo vệ và đủ điều kiện lập lịch: đã phê duyệt,
     * đã có nhóm thực hiện, có giảng viên hướng dẫn chính đang hoạt động và chưa
     * được gắn vào lịch bảo vệ khác.
     */
    public List<TopicResponse> getEligibleForDefenseSchedule(Long defensePeriodId) {
        if (defensePeriodId == null || !defensePeriodRepository.existsById(defensePeriodId)) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND);
        }
        return topicRepository
                .findEligibleForDefenseSchedule(
                        defensePeriodId,
                        List.of(
                                TopicStatusConstain.APPROVED,
                                TopicStatusConstain.REGISTERED,
                                TopicStatusConstain.IN_PROGRESS),
                        SupervisorAssignmentStatusConstain.ACTIVE,
                        SupervisorRoleConstain.PRIMARY)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_STUDENT')")
    @Transactional
    // Hàm updateTopic: Nhận mã bản ghi cùng dữ liệu cập nhật của updateTopic, tải bản ghi hiện có, kiểm tra trạng thái
    // và ràng buộc rồi ghi các giá trị mới xuống repository.
    public TopicResponse updateTopic(Long topicId, UpdateTopicRequest request) {
        return updateTopicInternal(topicId, request);
    }

    /** Cập nhật thông tin đề tài và thay thế tệp mô tả khi người dùng gửi tệp mới trong request multipart. */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_STUDENT')")
    @Transactional
    public TopicResponse updateTopic(Long topicId, UpdateTopicRequest request, MultipartFile file) {
        validateTopicFile(file);
        TopicResponse response = updateTopicInternal(topicId, request);
        if (file == null || file.isEmpty()) {
            return response;
        }
        TopicEntity topic = findTopic(topicId);
        topic.setFileData(encodeTopicFile(file));
        topicRepository.save(topic);
        return toResponse(topic);
    }

    private TopicResponse updateTopicInternal(Long topicId, UpdateTopicRequest request) {
        TopicEntity topic = findTopic(topicId);
        requireOwnerOrManager(topic);
        requireStudentParticipation(topic);
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
        return toResponse(topicRepository.save(topic));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_STUDENT')")
    @Transactional
    // Hàm deleteTopic: Nhận mã bản ghi của deleteTopic, kiểm tra quyền và các quan hệ đang sử dụng, sau đó xóa hoặc
    // chuyển bản ghi sang trạng thái tương ứng.
    public void deleteTopic(Long topicId) {
        TopicEntity topic = findTopic(topicId);
        requireOwnerOrManager(topic);
        requireStudentParticipation(topic);
        if (topic.getTeam() != null
                || !topic.getTopicSuperVisorEntities().isEmpty()
                || !topic.getReviewAssignment().isEmpty()
                || topic.getDefenseSchedule() != null) {
            throw new AppException(ErrorCode.TOPIC_IN_USE);
        }
        requireEditable(topic);
        topicRepository.delete(topic);
        topicRepository.flush();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_STUDENT')")
    @Transactional
    // Hàm submitForApproval: Nhận dữ liệu đầu vào của submitForApproval, kiểm tra các trường bắt buộc và quan hệ liên
    // quan, tạo bản ghi nghiệp vụ rồi lưu repository để trả kết quả cho API.
    public TopicResponse submitForApproval(Long topicId) {
        TopicEntity topic = findTopic(topicId);
        requireOwnerOrManager(topic);
        if (hasAuthority("ROLE_STUDENT")) {
            requireStudentTeamWithoutTopic();
            StudentEntity currentStudent = studentRepository
                    .findByUserEntity_UserId(currentUserId())
                    .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));
            graduationEnrollmentService.requireParticipationAllowed(
                    currentStudent.getIdStudent(), topic.getDefensePeriod().getID_Defense());
        }
        requireEditable(topic);
        requireActiveDefensePeriod(topic.getDefensePeriod());
        topic.setStatus(TopicStatusConstain.PENDING_APPROVAL);
        topic.setRejectionReason(null);
        return toResponse(topicRepository.save(topic));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm approveTopic: Nhận mã bản ghi và thông tin thao tác của approveTopic, kiểm tra trạng thái hiện tại cùng quyền
    // thực hiện, cập nhật trạng thái/lý do và lưu thay đổi.
    public TopicResponse approveTopic(Long topicId) {
        TopicEntity topic = findTopic(topicId);
        // Topics created from the administration screen are already under review;
        // administrators/faculty may approve them directly from DRAFT. Student
        // proposals continue to use the normal PENDING_APPROVAL state.
        if (topic.getStatus() != TopicStatusConstain.PENDING_APPROVAL
                && topic.getStatus() != TopicStatusConstain.DRAFT) {
            throw new AppException(ErrorCode.TOPIC_OPERATION_NOT_ALLOWED);
        }
        // Đề tài được Admin tạo từ module Quản lý đề tài ở trạng thái DRAFT
        // là kho đề tài dùng để gán nhóm sau này. Category chỉ mô tả nguồn ý
        // tưởng, không đồng nghĩa đây là một yêu cầu đề xuất kèm nhóm. Vì vậy
        // không bắt buộc có team khi Admin phê duyệt một DRAFT.
        if (topic.getStatus() == TopicStatusConstain.DRAFT) {
            topic.setStatus(TopicStatusConstain.APPROVED);
        } else if (topic.getCategoryTopic() == CategoryTopicConstain.LECTURER
                || topic.getCategoryTopic() == CategoryTopicConstain.STUDENT) {
            TeamEntity team = topicRegistrationRepository
                    .findFirstByTopic_IdTopicAndStatusOrderBySubmittedAtDesc(
                            topicId, TopicRegistrationStatusConstain.PENDING)
                    .map(TopicRegistrationEntity::getTeam)
                    .orElseGet(() -> teamRepository
                            .findByStudentEntities_UserEntity_UserId(topic.getCreatedBy())
                            .orElseThrow(() -> new AppException(ErrorCode.STUDENT_TEAM_REQUIRED)));
            if (team.getTopic() != null) throw new AppException(ErrorCode.TEAM_ALREADY_HAS_TOPIC);
            if (teamRepository.existsByTopic_IdTopic(topicId)) throw new AppException(ErrorCode.TOPIC_ALREADY_ASSIGNED);
            team.setTopic(topic);
            topic.setTeam(team);
            topic.setStatus(TopicStatusConstain.APPROVED);
            teamRepository.save(team);
            graduationEnrollmentService.autoEnrollStudents(
                    team.getStudentEntities(), topic.getDefensePeriod(), "Tự động ghi danh khi Admin phê duyệt đề tài");
            if (topic.getCategoryTopic() == CategoryTopicConstain.LECTURER) {
                assignProposingLecturer(topic);
            }
            topicRegistrationRepository
                    .findByTopic_IdTopicAndStatus(topicId, TopicRegistrationStatusConstain.PENDING)
                    .forEach(registration -> {
                        registration.setStatus(TopicRegistrationStatusConstain.APPROVED);
                        registration.setReviewedAt(java.time.LocalDateTime.now());
                        registration.setReviewedBy(currentUserEntity());
                        registration.setRejectionReason(null);
                    });
        } else {
            topic.setStatus(TopicStatusConstain.APPROVED);
        }
        topic.setRejectionReason(null);
        return toResponse(topicRepository.save(topic));
    }

    /** Tự động tạo phân công hướng dẫn chính cho giảng viên đã gửi đề xuất sau khi admin duyệt đề tài. */
    private void assignProposingLecturer(TopicEntity topic) {
        LectureEntity lecture = lectureRepository
                .findByUser_UserId(topic.getCreatedBy())
                .orElseThrow(() -> new AppException(ErrorCode.LECTURER_PROFILE_NOT_FOUND));
        if (lecture.getUser() == null
                || lecture.getUser().getStatus() == com.graduration.Constain.StatusConstain.INACTIVE
                || lecture.getUser().getStatus() == com.graduration.Constain.StatusConstain.DELETED) {
            throw new AppException(ErrorCode.LECTURER_INACTIVE);
        }
        if (topicSupervisorRepository.existsByTopic_IdTopicAndLecture_LectureIdAndStatus(
                topic.getIdTopic(), lecture.getLectureId(), SupervisorAssignmentStatusConstain.ACTIVE)) {
            return;
        }
        if (topicSupervisorRepository.existsByTopic_IdTopicAndSupervisorRoleAndStatus(
                topic.getIdTopic(), SupervisorRoleConstain.PRIMARY, SupervisorAssignmentStatusConstain.ACTIVE)) {
            throw new AppException(ErrorCode.TOPIC_PRIMARY_SUPERVISOR_ALREADY_EXISTS);
        }
        TopicSuperVisorEntity assignment = TopicSuperVisorEntity.builder()
                .topic(topic)
                .lecture(lecture)
                .supervisorRole(SupervisorRoleConstain.PRIMARY)
                .status(SupervisorAssignmentStatusConstain.ACTIVE)
                .assignedAt(java.time.LocalDateTime.now())
                .assignedBy(currentUserEntity())
                .note("Tự động phân công giảng viên đề xuất sau khi duyệt")
                .build();
        topicSupervisorRepository.save(assignment);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    // Hàm rejectTopic: Nhận mã bản ghi và thông tin thao tác của rejectTopic, kiểm tra trạng thái hiện tại cùng quyền
    // thực hiện, cập nhật trạng thái/lý do và lưu thay đổi.
    public TopicResponse rejectTopic(Long topicId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new AppException(ErrorCode.TOPIC_REJECTION_REASON_NOT_BLANK);
        }
        TopicEntity topic = findTopic(topicId);
        requireStatus(topic, TopicStatusConstain.PENDING_APPROVAL);
        topic.setStatus(TopicStatusConstain.REJECTED);
        topic.setRejectionReason(reason.trim());
        // Khi đề xuất của giảng viên bị từ chối, giải phóng thành viên và xóa nhóm tạm.
        if (topic.getCategoryTopic() == CategoryTopicConstain.LECTURER && topic.getTeam() == null) {
            TeamEntity proposedTeam = topicRegistrationRepository
                    .findFirstByTopic_IdTopicAndStatusOrderBySubmittedAtDesc(
                            topicId, TopicRegistrationStatusConstain.PENDING)
                    .map(TopicRegistrationEntity::getTeam)
                    .orElse(null);
            if (proposedTeam == null) {
                return toResponse(topicRepository.save(topic));
            }
            List<StudentEntity> proposedStudents = new ArrayList<>(proposedTeam.getStudentEntities());
            proposedStudents.forEach(student -> {
                student.getTeamMemberships().remove(proposedTeam);
            });
            studentRepository.saveAll(proposedStudents);
            topicRegistrationRepository
                    .findByTopic_IdTopicAndStatus(topicId, TopicRegistrationStatusConstain.PENDING)
                    .forEach(registration -> {
                        registration.setStatus(TopicRegistrationStatusConstain.REJECTED);
                        registration.setReviewedAt(java.time.LocalDateTime.now());
                        registration.setReviewedBy(currentUserEntity());
                        registration.setRejectionReason(reason.trim());
                        registration.setTeam(null);
                    });
            topicRegistrationRepository.flush();
            teamRepository.delete(proposedTeam);
        }
        return toResponse(topicRepository.save(topic));
    }

    // Hàm findTopic: Nhận mã hoặc điều kiện tìm kiếm của findTopic, truy vấn bản ghi/quan hệ tương ứng, báo lỗi khi
    // không tồn tại và trả về dữ liệu đã ánh xạ.
    private TopicEntity findTopic(Long topicId) {
        if (topicId == null) {
            throw new AppException(ErrorCode.TOPIC_NOT_FOUND);
        }
        return topicRepository.findById(topicId).orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
    }

    // Hàm toResponse: Nhận entity nghiệp vụ; lấy các quan hệ liên quan và ánh xạ mã, tên, trạng thái, thời gian cùng
    // thông tin hiển thị sang DTO response cho API.
    private TopicResponse toResponse(TopicEntity topic) {
        TopicResponse response = topicMapper.toResponse(topic);
        withFileMetadata(response, topic);
        TeamEntity associatedTeam = topic.getTeam();
        if (associatedTeam == null) {
            associatedTeam = topicRegistrationRepository
                    .findFirstByTopic_IdTopicAndStatusOrderBySubmittedAtDesc(
                            topic.getIdTopic(), TopicRegistrationStatusConstain.PENDING)
                    .map(TopicRegistrationEntity::getTeam)
                    .orElse(null);
        }
        if (associatedTeam == null && topic.getCategoryTopic() == CategoryTopicConstain.STUDENT) {
            associatedTeam = teamRepository
                    .findByStudentEntities_UserEntity_UserId(topic.getCreatedBy())
                    .orElse(null);
        }
        if (associatedTeam != null) {
            response.setTeamId(associatedTeam.getIdTeam());
            response.setTeamName(associatedTeam.getNameTeam());
            // Nạp quan hệ sinh viên bằng EntityGraph để chi tiết đề xuất luôn
            // hiển thị đúng những người được giảng viên chọn vào nhóm.
            TeamEntity detailedTeam = associatedTeam.getIdTeam() == null
                    ? associatedTeam
                    : teamRepository
                            .findWithDetailsByIdTeam(associatedTeam.getIdTeam())
                            .orElse(associatedTeam);
            response.setProposedStudents(toStudentSummaries(detailedTeam.getStudentEntities()));
        }
        if (topic.getCreatedBy() == null || topic.getCreatedBy().isBlank()) {
            return response;
        }
        // TopicMapper bỏ qua trường createdBy để không ghi ngược định danh khi
        // cập nhật; khi trả response cần giữ lại định danh gốc để tra cứu tên
        // hiển thị và không làm mất dữ liệu mà các client khác đang sử dụng.
        response.setCreatedBy(topic.getCreatedBy());
        // Luôn ưu tiên tên trong hồ sơ giảng viên/sinh viên. Một số dữ liệu cũ
        // chỉ lưu UUID user_id và không khởi tạo quan hệ ngược trên UserEntity,
        // vì vậy phải tra cứu trực tiếp hồ sơ trước khi dùng fallback tài khoản.
        resolveCreatorName(topic.getCreatedBy()).ifPresent(response::setCreatedByName);
        return response;
    }

    /** Chuyển danh sách StudentEntity của nhóm sang DTO tối giản để FE hiển thị tên, mã và liên hệ. */
    private List<TeamResponse.StudentSummary> toStudentSummaries(List<StudentEntity> students) {
        if (students == null || students.isEmpty()) {
            return new ArrayList<>();
        }
        return students.stream()
                .filter(Objects::nonNull)
                .map(student -> TeamResponse.StudentSummary.builder()
                        .studentCode(student.getStudentCode())
                        .fullName(student.getFullNameStudent())
                        .email(student.getEmail())
                        .classId(
                                student.getClassEntity() == null
                                        ? null
                                        : student.getClassEntity().getClassId())
                        .classCode(
                                student.getClassEntity() == null
                                        ? null
                                        : student.getClassEntity().getClassCode())
                        .build())
                .toList();
    }

    /**
     * Tìm tài khoản tạo đề tài theo định danh đang lưu trong topic; hỗ trợ cả
     * userId, username, mã giảng viên và mã sinh viên để dữ liệu cũ vẫn hiển thị
     * đúng tên thay vì lộ UUID trên màn hình duyệt đăng ký.
     */
    private Optional<UserEntity> resolveCreator(String creator) {
        if (creator == null || creator.isBlank()) {
            return Optional.empty();
        }
        String value = creator.trim();
        // Tra cứu trực tiếp hồ sơ trước để không phụ thuộc vào quan hệ ngược
        // UserEntity.lecture/student (có thể chưa được nạp ở dữ liệu cũ).
        Optional<LectureEntity> lectureByUserId = lectureRepository.findByUser_UserId(value);
        if (lectureByUserId.isPresent() && lectureByUserId.get().getUser() != null) {
            return Optional.of(lectureByUserId.get().getUser());
        }
        Optional<StudentEntity> studentByUserId = studentRepository.findByUserEntity_UserId(value);
        if (studentByUserId.isPresent() && studentByUserId.get().getUserEntity() != null) {
            return Optional.of(studentByUserId.get().getUserEntity());
        }
        Optional<LectureEntity> lectureByUserName = lectureRepository.findByUser_UserName(value);
        if (lectureByUserName.isPresent() && lectureByUserName.get().getUser() != null) {
            return Optional.of(lectureByUserName.get().getUser());
        }
        Optional<StudentEntity> studentByUserName = studentRepository.findByUserEntity_UserName(value);
        if (studentByUserName.isPresent() && studentByUserName.get().getUserEntity() != null) {
            return Optional.of(studentByUserName.get().getUserEntity());
        }
        // Các topic cũ thường lưu trực tiếp user_id (UUID). Tra cứu đúng khóa
        // chính như bước cuối của nhóm định danh user.
        Optional<UserEntity> byUserId = userRepository.findById(value);
        if (byUserId.isPresent()) {
            return byUserId;
        }
        // Tra cứu một lần theo cả user_id, username, lecture_id/lecture_code và
        // student_id/student_code để tương thích với mọi dữ liệu đã tồn tại.
        Optional<UserEntity> byAnyIdentifier = userRepository.findByAnyIdentifier(value);
        if (byAnyIdentifier.isPresent()) {
            return byAnyIdentifier;
        }
        // Một số dữ liệu cũ lưu lecture_id/student_id thay vì user_id; tra cứu
        // trực tiếp hồ sơ tương ứng để vẫn lấy được tên người đề xuất.
        Optional<LectureEntity> byLectureId = lectureRepository.findById(value);
        if (byLectureId.isPresent() && byLectureId.get().getUser() != null) {
            return Optional.of(byLectureId.get().getUser());
        }
        Optional<StudentEntity> byStudentId = studentRepository.findById(value);
        if (byStudentId.isPresent() && byStudentId.get().getUserEntity() != null) {
            return Optional.of(byStudentId.get().getUserEntity());
        }
        // Fallback cho mã sinh viên không phân biệt hoa thường ở dữ liệu cũ.
        return studentRepository.findByStudentCodeIgnoreCase(value).map(StudentEntity::getUserEntity);
    }

    /**
     * Lấy tên hiển thị của người tạo đề tài từ hồ sơ giảng viên hoặc sinh viên.
     * Định danh trong dữ liệu cũ có thể là user_id, lecture_id, student_id hoặc
     * mã sinh viên nên lần lượt thử từng khóa trước khi dùng thông tin tài khoản.
     */
    private Optional<String> resolveCreatorName(String creator) {
        if (creator == null || creator.isBlank()) {
            return Optional.empty();
        }
        String value = creator.trim();
        // Truy vấn một lần theo toàn bộ khóa định danh giúp xử lý cả dữ liệu
        // mới và các bản ghi cũ lưu lecture_id/student_id thay vì user_id.
        Optional<String> directLectureName =
                lectureRepository.findDisplayNameByAnyIdentifier(value).filter(name -> name != null && !name.isBlank());
        if (directLectureName.isPresent()) {
            return directLectureName;
        }
        Optional<String> directStudentName =
                studentRepository.findDisplayNameByAnyIdentifier(value).filter(name -> name != null && !name.isBlank());
        if (directStudentName.isPresent()) {
            return directStudentName;
        }
        Optional<String> lectureName = lectureRepository
                .findByUser_UserId(value)
                .map(LectureEntity::getFullNameLecture)
                .filter(name -> name != null && !name.isBlank());
        if (lectureName.isPresent()) {
            return lectureName;
        }
        Optional<String> studentName = studentRepository
                .findByUserEntity_UserId(value)
                .map(StudentEntity::getFullNameStudent)
                .filter(name -> name != null && !name.isBlank());
        if (studentName.isPresent()) {
            return studentName;
        }
        Optional<String> lectureUserName = lectureRepository
                .findByUser_UserName(value)
                .map(LectureEntity::getFullNameLecture)
                .filter(name -> name != null && !name.isBlank());
        if (lectureUserName.isPresent()) {
            return lectureUserName;
        }
        Optional<String> studentUserName = studentRepository
                .findByUserEntity_UserName(value)
                .map(StudentEntity::getFullNameStudent)
                .filter(name -> name != null && !name.isBlank());
        if (studentUserName.isPresent()) {
            return studentUserName;
        }
        Optional<String> lectureIdName = lectureRepository
                .findById(value)
                .map(LectureEntity::getFullNameLecture)
                .filter(name -> name != null && !name.isBlank());
        if (lectureIdName.isPresent()) {
            return lectureIdName;
        }
        Optional<String> studentIdName = studentRepository
                .findById(value)
                .map(StudentEntity::getFullNameStudent)
                .filter(name -> name != null && !name.isBlank());
        if (studentIdName.isPresent()) {
            return studentIdName;
        }
        Optional<String> studentCodeName = studentRepository
                .findByStudentCodeIgnoreCase(value)
                .map(StudentEntity::getFullNameStudent)
                .filter(name -> name != null && !name.isBlank());
        if (studentCodeName.isPresent()) {
            return studentCodeName;
        }
        // Dữ liệu legacy đôi khi lưu lecture_id/student_id hoặc user_id nhưng
        // quan hệ tìm kiếm tương ứng không còn khớp. Quét các hồ sơ đã nạp
        // đầy đủ làm bước cuối để vẫn hiển thị đúng họ tên thay vì UUID.
        Optional<String> profileName = lectureRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(lecture -> matchesCreatorIdentifier(
                        value,
                        lecture.getLectureId(),
                        lecture.getLectureCode(),
                        lecture.getUser() == null ? null : lecture.getUser().getUserId(),
                        lecture.getUser() == null ? null : lecture.getUser().getUserName()))
                .map(LectureEntity::getFullNameLecture)
                .filter(name -> name != null && !name.isBlank())
                .findFirst();
        if (profileName.isPresent()) {
            return profileName;
        }
        profileName = studentRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(student -> matchesCreatorIdentifier(
                        value,
                        student.getIdStudent(),
                        student.getStudentCode(),
                        student.getUserEntity() == null
                                ? null
                                : student.getUserEntity().getUserId(),
                        student.getUserEntity() == null
                                ? null
                                : student.getUserEntity().getUserName()))
                .map(StudentEntity::getFullNameStudent)
                .filter(name -> name != null && !name.isBlank())
                .findFirst();
        if (profileName.isPresent()) {
            return profileName;
        }
        // Bước dự phòng cuối cùng: nạp các tài khoản kèm hồ sơ và đối chiếu
        // toàn bộ định danh. Cách này vẫn xử lý được dữ liệu cũ khi khóa được
        // lưu ở topic không trùng với trường mà một truy vấn cụ thể dự đoán.
        profileName = userRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(user -> matchesCreatorIdentifier(
                        value,
                        user.getUserId(),
                        user.getUserName(),
                        user.getLecture() == null ? null : user.getLecture().getLectureId(),
                        user.getLecture() == null ? null : user.getLecture().getLectureCode(),
                        user.getStudent() == null ? null : user.getStudent().getIdStudent(),
                        user.getStudent() == null ? null : user.getStudent().getStudentCode()))
                .map(this::displayName)
                .filter(name -> name != null && !name.isBlank() && !name.equals(value))
                .findFirst();
        if (profileName.isPresent()) {
            return profileName;
        }
        return resolveCreator(value)
                .map(this::displayName)
                .filter(name -> name != null && !name.isBlank() && !name.equals(value));
    }

    private boolean matchesCreatorIdentifier(String value, String... candidates) {
        if (value == null || value.isBlank() || candidates == null) {
            return false;
        }
        for (String candidate : candidates) {
            if (candidate != null && value.equalsIgnoreCase(candidate.trim())) {
                return true;
            }
        }
        return false;
    }

    private TopicResponse withFileMetadata(TopicResponse response, TopicEntity topic) {
        if (response == null || response.getTopicId() == null) {
            return response;
        }
        decodeTopicFile(topic == null ? null : topic.getFileData()).ifPresent(file -> {
            response.setFileName(file.fileName());
            response.setContentType(file.contentType());
            response.setFileSize((long) file.content().length);
        });
        return response;
    }

    /** Đóng gói tên, MIME và bytes của tệp thành một payload nhị phân để lưu trong cột file_data. */
    private byte[] encodeTopicFile(MultipartFile file) {
        try {
            byte[] content = file.getBytes();
            String fileName = file.getOriginalFilename() == null
                            || file.getOriginalFilename().isBlank()
                    ? "de-tai"
                    : file.getOriginalFilename().replace('\\', '/');
            fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
            String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(content.length + 128);
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(0x54465031); // TFP1
                writeText(output, fileName);
                writeText(output, contentType);
                output.writeInt(content.length);
                output.write(content);
            }
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new AppException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    /** Đọc payload file_data, kiểm tra kích thước và tách metadata khỏi nội dung file để trả về API. */
    private java.util.Optional<TopicFile> decodeTopicFile(byte[] payload) {
        if (payload == null || payload.length < 16) return java.util.Optional.empty();
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            if (input.readInt() != 0x54465031) return java.util.Optional.empty();
            String fileName = readText(input, 1024);
            String contentType = readText(input, 255);
            int size = input.readInt();
            if (size < 0 || size > MAX_TOPIC_FILE_SIZE || size > input.available()) return java.util.Optional.empty();
            byte[] content = input.readNBytes(size);
            return java.util.Optional.of(new TopicFile(fileName, contentType, content));
        } catch (IOException | RuntimeException exception) {
            return java.util.Optional.empty();
        }
    }

    private void writeText(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private String readText(DataInputStream input, int maxLength) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > maxLength || length > input.available())
            throw new IOException("Invalid file metadata");
        return new String(input.readNBytes(length), StandardCharsets.UTF_8);
    }

    private void validateTopicFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        if (file.getSize() > MAX_TOPIC_FILE_SIZE) {
            throw new AppException(ErrorCode.TOPIC_FILE_TOO_LARGE);
        }
        if (!ALLOWED_TOPIC_FILE_EXTENSIONS.contains(extension(file.getOriginalFilename()))) {
            throw new AppException(ErrorCode.TOPIC_FILE_TYPE_NOT_ALLOWED);
        }
    }

    private String extension(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    // Hàm displayName: Nhận UserEntity; ưu tiên tên hiển thị của hồ sơ sinh viên hoặc giảng viên, sau đó mới dùng
    // username để tạo tên người thực hiện trong phản hồi/audit.
    private String displayName(UserEntity user) {
        if (user.getLecture() != null
                && user.getLecture().getFullNameLecture() != null
                && !user.getLecture().getFullNameLecture().isBlank()) {
            return user.getLecture().getFullNameLecture();
        }
        if (user.getStudent() != null
                && user.getStudent().getFullNameStudent() != null
                && !user.getStudent().getFullNameStudent().isBlank()) {
            return user.getStudent().getFullNameStudent();
        }
        return user.getUserName();
    }

    // Hàm findActiveDefensePeriod: Nhận mã hoặc điều kiện tìm kiếm của findActiveDefensePeriod, truy vấn bản ghi/quan
    // hệ tương ứng, báo lỗi khi không tồn tại và trả về dữ liệu đã ánh xạ.
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

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireActiveDefensePeriod(DefensePeriodEntity period) {
        if (period.getStatus() == DefensePeriodConstain.FINISHED) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_FINISHED);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void validateRequest(String title, CategoryTopicConstain category) {
        if (title == null || title.isBlank()) {
            throw new AppException(ErrorCode.TOPIC_TITLE_NOT_BLANK);
        }
        if (category == null) {
            throw new AppException(ErrorCode.TOPIC_CATEGORY_NOT_BLANK);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireEditable(TopicEntity topic) {
        if (topic.getStatus() != TopicStatusConstain.DRAFT && topic.getStatus() != TopicStatusConstain.REJECTED) {
            throw new AppException(ErrorCode.TOPIC_OPERATION_NOT_ALLOWED);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireStatus(TopicEntity topic, TopicStatusConstain expected) {
        if (topic.getStatus() != expected) {
            throw new AppException(ErrorCode.TOPIC_OPERATION_NOT_ALLOWED);
        }
    }

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private void requireOwnerOrManager(TopicEntity topic) {
        Authentication authentication = currentAuthentication();
        boolean manager = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_FACULTY"));
        if (!manager && !Objects.equals(topic.getCreatedBy(), authentication.getName())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    // Hàm currentUserId: Lấy tên định danh của tài khoản đã đăng nhập từ SecurityContext; dùng định danh này để truy
    // vấn hồ sơ và giới hạn dữ liệu theo người dùng hiện tại.
    private String currentUserId() {
        return currentAuthentication().getName();
    }

    /** Tải đầy đủ tài khoản đang đăng nhập để gắn người thực hiện vào bản ghi phân công tự động. */
    private UserEntity currentUserEntity() {
        return userRepository.findById(currentUserId()).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
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

    // Kiểm tra các điều kiện và quy tắc nghiệp vụ trước khi tiếp tục xử lý.
    private TeamEntity requireStudentTeamWithoutTopic() {
        TeamEntity team = teamRepository
                .findByStudentEntities_UserEntity_UserId(currentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_TEAM_REQUIRED));
        if (team.getTopic() != null) {
            throw new AppException(ErrorCode.TEAM_ALREADY_HAS_TOPIC);
        }
        return team;
    }

    private void requireStudentParticipation(TopicEntity topic) {
        if (!hasAuthority("ROLE_STUDENT") || topic.getDefensePeriod() == null) {
            return;
        }
        StudentEntity currentStudent = studentRepository
                .findByUserEntity_UserId(currentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));
        graduationEnrollmentService.requireParticipationAllowed(
                currentStudent.getIdStudent(), topic.getDefensePeriod().getID_Defense());
    }

    // Hàm hasAuthority: Nhận tên quyền cần kiểm tra; so sánh với danh sách GrantedAuthority của tài khoản hiện tại và
    // trả về true nếu tài khoản có quyền đó.
    private boolean hasAuthority(String authority) {
        return currentAuthentication().getAuthorities().stream()
                .anyMatch(item -> item.getAuthority().equals(authority));
    }

    // Hàm normalize: Nhận CreateTopicRequest hoặc UpdateTopicRequest; chuẩn hóa mô tả, mục tiêu và công nghệ bằng cách
    // trim chuỗi có nội dung, đổi chuỗi trống thành null trước khi lưu đề tài.
    private void normalize(CreateTopicRequest request) {
        request.setDescription(normalize(request.getDescription()));
        request.setObjective(normalize(request.getObjective()));
        request.setTechnology(normalize(request.getTechnology()));
    }

    // Hàm normalize: Nhận CreateTopicRequest hoặc UpdateTopicRequest; chuẩn hóa mô tả, mục tiêu và công nghệ bằng cách
    // trim chuỗi có nội dung, đổi chuỗi trống thành null trước khi lưu đề tài.
    private void normalize(UpdateTopicRequest request) {
        request.setDescription(normalize(request.getDescription()));
        request.setObjective(normalize(request.getObjective()));
        request.setTechnology(normalize(request.getTechnology()));
    }

    // Hàm normalize: Nhận CreateTopicRequest hoặc UpdateTopicRequest; chuẩn hóa mô tả, mục tiêu và công nghệ bằng cách
    // trim chuỗi có nội dung, đổi chuỗi trống thành null trước khi lưu đề tài.
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // Hàm ImportTopicResult: Đóng gói tổng số dòng đề tài, số dòng import thành công/thất bại, các đề tài đã tạo và lỗi
    // theo dòng để trả về sau khi xử lý Excel.
    public record ImportTopicResult(
            int totalRows,
            int importedRows,
            int failedRows,
            List<TopicResponse> importedTopics,
            List<ImportTopicError> errors) {}

    // Hàm ImportTopicError: Đóng gói số dòng Excel, giá trị tiêu đề và thông báo lỗi của đề tài không thể import để
    // giao diện hiển thị chi tiết.
    public record ImportTopicError(int row, String title, String message) {}

    public record DownloadedTopic(Resource resource, String fileName, String contentType) {}

    private record TopicFile(String fileName, String contentType, byte[] content) {}
}

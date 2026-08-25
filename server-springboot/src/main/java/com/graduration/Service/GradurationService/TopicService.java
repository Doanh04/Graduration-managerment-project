package com.graduration.Service.GradurationService;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import com.graduration.Constain.TopicStatusConstain;
import com.graduration.DTO.Request.CreateTopicRequest;
import com.graduration.DTO.Request.UpdateTopicRequest;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.DTO.Response.TopicResponse;
import com.graduration.Repository.DefensePeriodRepository;
import com.graduration.Repository.TeamRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.TeamEntity;
import com.graduration.entity.TopicEntity;
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
    TopicRepository topicRepository;
    DefensePeriodRepository defensePeriodRepository;
    UserRepository userRepository;
    TeamRepository teamRepository;
    TopicMapper topicMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_STUDENT')")
    @Transactional
    public TopicResponse createTopic(CreateTopicRequest request) {
        return createTopicInternal(request);
    }

    private TopicResponse createTopicInternal(CreateTopicRequest request) {
        TeamEntity proposingTeam = null;
        if (hasAuthority("ROLE_STUDENT")) {
            proposingTeam = requireStudentTeamWithoutTopic();
            if (request == null || request.getCategoryTopic() != CategoryTopicConstain.STUDENT) {
                throw new AppException(ErrorCode.TOPIC_CATEGORY_NOT_BLANK);
            }
            if (topicRepository.existsByProposedTeam_IdTeamAndStatusIn(
                            proposingTeam.getIdTeam(),
                            List.of(TopicStatusConstain.DRAFT, TopicStatusConstain.PENDING_APPROVAL))
                    || topicRepository.findStudentProposalsByTeam(proposingTeam.getIdTeam()).stream()
                            .anyMatch(topic -> topic.getStatus() == TopicStatusConstain.DRAFT
                                    || topic.getStatus() == TopicStatusConstain.PENDING_APPROVAL)) {
                throw new AppException(ErrorCode.TOPIC_REGISTRATION_ALREADY_PENDING);
            }
        }
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
        topic.setProposedTeam(proposingTeam);
        topic.setStatus(TopicStatusConstain.DRAFT);
        return toResponse(topicRepository.save(topic));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_STUDENT')")
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

    private Long parseDefensePeriodId(String value) {
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND);
        }
    }

    private String cellValue(Row row, int column, DataFormatter formatter) {
        return formatter.formatCellValue(row.getCell(column)).trim();
    }

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
    public TopicResponse getTopic(Long topicId) {
        return toResponse(findTopic(topicId));
    }

    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Transactional
    public List<TopicResponse> getMyProposals() {
        TeamEntity team = teamRepository
                .findByStudentEntities_UserEntity_UserId(currentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_TEAM_REQUIRED));
        List<TopicEntity> proposals = topicRepository.findStudentProposalsByTeam(team.getIdTeam());
        proposals.stream()
                .filter(topic -> topic.getProposedTeam() == null)
                .forEach(topic -> topic.setProposedTeam(team));
        return proposals.stream().map(this::toResponse).toList();
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
        return toResponse(topicRepository.save(topic));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_STUDENT')")
    @Transactional
    public void deleteTopic(Long topicId) {
        TopicEntity topic = findTopic(topicId);
        requireOwnerOrManager(topic);
        if (topic.getTeam() != null
                || !topic.getTopicSuperVisorEntities().isEmpty()
                || !topic.getReviewAssignment().isEmpty()
                || topic.getDefenseSchedule() != null) {
            throw new AppException(ErrorCode.TOPIC_IN_USE);
        }
        requireEditable(topic);
        topicRepository.delete(topic);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR', 'ROLE_STUDENT')")
    @Transactional
    public TopicResponse submitForApproval(Long topicId) {
        TopicEntity topic = findTopic(topicId);
        requireOwnerOrManager(topic);
        if (hasAuthority("ROLE_STUDENT")) {
            requireStudentTeamWithoutTopic();
        }
        requireEditable(topic);
        requireActiveDefensePeriod(topic.getDefensePeriod());
        topic.setStatus(TopicStatusConstain.PENDING_APPROVAL);
        topic.setRejectionReason(null);
        return toResponse(topicRepository.save(topic));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public TopicResponse approveTopic(Long topicId) {
        TopicEntity topic = findTopic(topicId);
        requireStatus(topic, TopicStatusConstain.PENDING_APPROVAL);
        if (topic.getCategoryTopic() == CategoryTopicConstain.STUDENT) {
            TeamEntity team = topic.getProposedTeam() != null
                    ? topic.getProposedTeam()
                    : teamRepository
                            .findByStudentEntities_UserEntity_UserId(topic.getCreatedBy())
                            .orElseThrow(() -> new AppException(ErrorCode.STUDENT_TEAM_REQUIRED));
            if (team.getTopic() != null) throw new AppException(ErrorCode.TEAM_ALREADY_HAS_TOPIC);
            if (teamRepository.existsByTopic_IdTopic(topicId)) throw new AppException(ErrorCode.TOPIC_ALREADY_ASSIGNED);
            team.setTopic(topic);
            topic.setTeam(team);
            topic.setStatus(TopicStatusConstain.REGISTERED);
            teamRepository.save(team);
        } else {
            topic.setStatus(TopicStatusConstain.APPROVED);
        }
        topic.setRejectionReason(null);
        return toResponse(topicRepository.save(topic));
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
        return toResponse(topicRepository.save(topic));
    }

    private TopicEntity findTopic(Long topicId) {
        if (topicId == null) {
            throw new AppException(ErrorCode.TOPIC_NOT_FOUND);
        }
        return topicRepository.findById(topicId).orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
    }

    private TopicResponse toResponse(TopicEntity topic) {
        TopicResponse response = topicMapper.toResponse(topic);
        if (topic.getTeam() == null && topic.getProposedTeam() != null) {
            response.setTeamId(topic.getProposedTeam().getIdTeam());
            response.setTeamName(topic.getProposedTeam().getNameTeam());
        } else if (topic.getTeam() == null && topic.getCategoryTopic() == CategoryTopicConstain.STUDENT) {
            teamRepository
                    .findByStudentEntities_UserEntity_UserId(topic.getCreatedBy())
                    .ifPresent(team -> {
                        response.setTeamId(team.getIdTeam());
                        response.setTeamName(team.getNameTeam());
                    });
        }
        if (topic.getCreatedBy() == null || topic.getCreatedBy().isBlank()) {
            return response;
        }
        userRepository.findById(topic.getCreatedBy()).ifPresent(user -> {
            String name = displayName(user);
            response.setCreatedByName(name);
            response.setCreatedBy(name);
        });
        return response;
    }

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

    private TeamEntity requireStudentTeamWithoutTopic() {
        TeamEntity team = teamRepository
                .findByStudentEntities_UserEntity_UserId(currentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_TEAM_REQUIRED));
        if (team.getTopic() != null) {
            throw new AppException(ErrorCode.TEAM_ALREADY_HAS_TOPIC);
        }
        return team;
    }

    private boolean hasAuthority(String authority) {
        return currentAuthentication().getAuthorities().stream()
                .anyMatch(item -> item.getAuthority().equals(authority));
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

    public record ImportTopicResult(
            int totalRows,
            int importedRows,
            int failedRows,
            List<TopicResponse> importedTopics,
            List<ImportTopicError> errors) {}

    public record ImportTopicError(int row, String title, String message) {}
}

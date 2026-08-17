package com.graduration.Service.DerpatmentService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.TopicStatusConstain;
import com.graduration.DTO.Request.TeamRequest;
import com.graduration.DTO.Response.TeamResponse;
import com.graduration.Repository.StudentRepository;
import com.graduration.Repository.TeamRepository;
import com.graduration.Repository.TopicRepository;
import com.graduration.entity.StudentEntity;
import com.graduration.entity.TeamEntity;
import com.graduration.entity.TopicEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.TeamMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TeamService {
    TeamRepository teamRepository;
    StudentRepository studentRepository;
    TopicRepository topicRepository;
    TeamMapper teamMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public TeamResponse createTeam(TeamRequest request) {
        normalizeRequest(request);
        if (teamRepository.existsByNameTeamIgnoreCase(request.getNameTeam())) {
            throw new AppException(ErrorCode.TEAM_ALREADY_EXISTS);
        }

        TeamEntity team = teamMapper.toTeamEntity(request);
        team.setTopic(resolveTopicForCreate(request.getTopicId()));
        return teamMapper.toTeamResponse(teamRepository.save(team));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional(readOnly = true)
    public TeamResponse getTeam(Long teamId) {
        return teamMapper.toTeamResponse(findTeam(teamId));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional(readOnly = true)
    public List<TeamResponse> getAllTeams() {
        return getAllTeams(0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional(readOnly = true)
    public List<TeamResponse> getAllTeams(Integer page, Integer size) {
        return teamRepository.findAllByOrderByIdTeamAsc(PaginationSupport.pageRequest(page, size)).stream()
                .map(teamMapper::toTeamResponse)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_SUPERVISOR')")
    @Transactional(readOnly = true)
    public com.graduration.DTO.Response.PageResponse<TeamResponse> getAllTeamsPage(Integer page, Integer size) {
        return com.graduration.DTO.Response.PageResponse.from(
                teamRepository.findAllByOrderByIdTeamAsc(PaginationSupport.pageRequest(page, size)),
                teamMapper::toTeamResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public TeamResponse updateTeam(Long teamId, TeamRequest request) {
        TeamEntity team = findTeam(teamId);
        normalizeRequest(request);
        if (teamRepository.existsByNameTeamIgnoreCaseAndIdTeamNot(request.getNameTeam(), teamId)) {
            throw new AppException(ErrorCode.TEAM_ALREADY_EXISTS);
        }
        if (request.getTopicId() != null
                && teamRepository.existsByTopic_IdTopicAndIdTeamNot(request.getTopicId(), teamId)) {
            throw new AppException(ErrorCode.TOPIC_ALREADY_ASSIGNED);
        }

        teamMapper.updateTeam(request, team);
        team.setTopic(resolveTopic(request.getTopicId()));
        return teamMapper.toTeamResponse(teamRepository.save(team));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_STUDENT')")
    @Transactional
    public TeamResponse selectTopic(Long teamId, Long topicId) {
        TeamEntity team = findTeam(teamId);
        requireTeamMemberOrManager(team);

        if (team.getTopic() != null) {
            if (team.getTopic().getIdTopic().equals(topicId)) {
                return teamMapper.toTeamResponse(team);
            }
            throw new AppException(ErrorCode.TEAM_ALREADY_HAS_TOPIC);
        }

        TopicEntity topic =
                topicRepository.findById(topicId).orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
        if (topic.getStatus() != TopicStatusConstain.APPROVED
                && topic.getStatus() != TopicStatusConstain.OPEN_FOR_REGISTRATION) {
            throw new AppException(ErrorCode.TOPIC_NOT_AVAILABLE);
        }
        if (teamRepository.existsByTopic_IdTopic(topicId)) {
            throw new AppException(ErrorCode.TOPIC_ALREADY_ASSIGNED);
        }

        team.setTopic(topic);
        topic.setTeam(team);
        topic.setStatus(TopicStatusConstain.REGISTERED);
        topicRepository.save(topic);
        return teamMapper.toTeamResponse(teamRepository.save(team));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public void deleteTeam(Long teamId) {
        TeamEntity team = findTeam(teamId);
        team.getStudentEntities().forEach(student -> student.setTeam(null));
        studentRepository.saveAll(team.getStudentEntities());
        team.getStudentEntities().clear();
        if (team.getTopic() != null) {
            team.getTopic().setTeam(null);
        }
        teamRepository.delete(team);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public TeamResponse addStudent(Long teamId, String studentCode) {
        TeamEntity team = findTeam(teamId);
        StudentEntity student = findStudentByCode(studentCode);
        attachStudent(team, student);
        studentRepository.save(student);
        return teamMapper.toTeamResponse(team);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public TeamResponse addStudents(Long teamId, Set<String> studentCodes) {
        if (studentCodes == null
                || studentCodes.isEmpty()
                || studentCodes.stream().anyMatch(this::isBlank)) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        TeamEntity team = findTeam(teamId);
        Set<String> uniqueCodes = studentCodes.stream()
                .map(String::trim)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<StudentEntity> students =
                uniqueCodes.stream().map(this::findStudentByCode).toList();
        students.forEach(student -> validateStudentAssignment(team, student));
        students.forEach(student -> attachStudent(team, student));
        studentRepository.saveAll(students);
        return teamMapper.toTeamResponse(team);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public TeamResponse removeStudent(Long teamId, String studentCode) {
        TeamEntity team = findTeam(teamId);
        StudentEntity student = findStudentByCode(studentCode);
        if (student.getTeam() == null || !teamId.equals(student.getTeam().getIdTeam())) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        student.setTeam(null);
        team.getStudentEntities()
                .removeIf(member -> student.getStudentCode().equalsIgnoreCase(member.getStudentCode()));
        studentRepository.save(student);
        return teamMapper.toTeamResponse(team);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public ImportTeamStudentsResponse importStudents(Long teamId, MultipartFile file) {
        validateExcelFile(file);
        TeamEntity team = findTeam(teamId);
        List<TeamResponse.StudentSummary> importedStudents = new ArrayList<>();
        List<ImportTeamStudentError> errors = new ArrayList<>();
        int totalRows = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            validateExcelHeader(sheet.getRow(0), formatter);
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                String studentCode = cellValue(row, 0, formatter);
                if (studentCode == null) {
                    continue;
                }
                totalRows++;
                try {
                    StudentEntity student = studentRepository
                            .findByStudentCodeIgnoreCase(studentCode)
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
                    attachStudent(team, student);
                    studentRepository.save(student);
                    importedStudents.add(teamMapper.toStudentSummary(student));
                } catch (RuntimeException exception) {
                    errors.add(new ImportTeamStudentError(rowIndex + 1, studentCode, exception.getMessage()));
                }
            }
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof AppException appException) {
                throw appException;
            }
            throw new AppException(ErrorCode.INVALID_EXCEL_FILE);
        }

        return new ImportTeamStudentsResponse(
                totalRows, importedStudents.size(), errors.size(), importedStudents, errors);
    }

    private TeamEntity findTeam(Long teamId) {
        if (teamId == null) {
            throw new AppException(ErrorCode.TEAM_NOT_FOUND);
        }
        return teamRepository
                .findWithDetailsByIdTeam(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.TEAM_NOT_FOUND));
    }

    private StudentEntity findStudentByCode(String studentCode) {
        if (isBlank(studentCode)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return studentRepository
                .findByStudentCodeIgnoreCase(studentCode.trim())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private void attachStudent(TeamEntity team, StudentEntity student) {
        validateStudentAssignment(team, student);
        if (student.getTeam() != null) {
            return;
        }
        student.setTeam(team);
        team.getStudentEntities().add(student);
    }

    private void validateStudentAssignment(TeamEntity team, StudentEntity student) {
        if (student.getTeam() != null
                && !team.getIdTeam().equals(student.getTeam().getIdTeam())) {
            throw new AppException(ErrorCode.STUDENT_ALREADY_IN_TEAM);
        }
    }

    private TopicEntity resolveTopicForCreate(Long topicId) {
        if (topicId != null && teamRepository.existsByTopic_IdTopic(topicId)) {
            throw new AppException(ErrorCode.TOPIC_ALREADY_ASSIGNED);
        }
        return resolveTopic(topicId);
    }

    private TopicEntity resolveTopic(Long topicId) {
        if (topicId == null) {
            return null;
        }
        return topicRepository.findById(topicId).orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
    }

    private void requireTeamMemberOrManager(TeamEntity team) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        boolean manager = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_FACULTY"));
        boolean member = team.getStudentEntities().stream()
                .anyMatch(student -> student.getUserEntity() != null
                        && authentication
                                .getName()
                                .equals(student.getUserEntity().getUserId()));
        if (!manager && !member) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void normalizeRequest(TeamRequest request) {
        if (request == null || isBlank(request.getNameTeam())) {
            throw new AppException(ErrorCode.TEAM_NAME_NOT_BLANK);
        }
        request.setNameTeam(request.getNameTeam().trim());
        request.setDescription(normalize(request.getDescription()));
        request.setRole(normalize(request.getRole()));
    }

    private String normalize(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void validateExcelFile(MultipartFile file) {
        if (file == null
                || file.isEmpty()
                || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new AppException(ErrorCode.INVALID_EXCEL_FILE);
        }
    }

    private void validateExcelHeader(Row header, DataFormatter formatter) {
        if (header == null
                || !"studentCode"
                        .equalsIgnoreCase(
                                formatter.formatCellValue(header.getCell(0)).trim())) {
            throw new AppException(ErrorCode.INVALID_EXCEL_FILE);
        }
    }

    private String cellValue(Row row, int column, DataFormatter formatter) {
        if (row == null) {
            return null;
        }
        return normalize(formatter.formatCellValue(row.getCell(column)));
    }

    public record ImportTeamStudentsResponse(
            int totalRows,
            int successRows,
            int failedRows,
            List<TeamResponse.StudentSummary> importedStudents,
            List<ImportTeamStudentError> errors) {}

    public record ImportTeamStudentError(int row, String studentCode, String message) {}
}

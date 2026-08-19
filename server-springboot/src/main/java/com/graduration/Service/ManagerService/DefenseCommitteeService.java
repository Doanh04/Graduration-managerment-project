package com.graduration.Service.ManagerService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.Constain.CommitteeMemberRoleConstain;
import com.graduration.Constain.CommitteeMemberStatusConstain;
import com.graduration.Constain.DefenseCommitteeStatusConstain;
import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.Constain.DefenseScheduleStatusConstain;
import com.graduration.Constain.StatusConstain;
import com.graduration.DTO.Request.DeactivateDefenseCommitteeRequest;
import com.graduration.DTO.Request.DefenseCommitteeRequest;
import com.graduration.DTO.Response.DefenseCommitteeResponse;
import com.graduration.DTO.Response.DefenseCommitteeValidationResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.Repository.DefenseCommitteeRepository;
import com.graduration.Repository.DefensePeriodRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.ComitteesMemberEntity;
import com.graduration.entity.DefenseCommitteesEntity;
import com.graduration.entity.DefensePeriodEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.DefenseCommitteeMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefenseCommitteeService {
    private static final int MINIMUM_ACTIVE_MEMBERS = 3;

    DefenseCommitteeRepository committeeRepository;
    DefensePeriodRepository defensePeriodRepository;
    UserRepository userRepository;
    DefenseCommitteeMapper committeeMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public DefenseCommitteeResponse create(Long defensePeriodId, DefenseCommitteeRequest request) {
        DefensePeriodEntity period = findOpenPeriod(defensePeriodId);
        validateRequest(request);
        String name = request.getCommitteeName().trim();
        if (committeeRepository.existsNameInDefensePeriod(name, defensePeriodId)) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_ALREADY_EXISTS);
        }
        DefenseCommitteesEntity committee = DefenseCommitteesEntity.builder()
                .comitteesName(name)
                .description(normalize(request.getDescription()))
                .status(DefenseCommitteeStatusConstain.DRAFT)
                .defensePeriod(period)
                .academicYear(period.getAcademicYear())
                .createdBy(currentUser())
                .build();
        return committeeMapper.toResponse(committeeRepository.save(committee));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public PageResponse<DefenseCommitteeResponse> getByDefensePeriod(
            Long defensePeriodId, DefenseCommitteeStatusConstain status, String keyword, Integer page, Integer size) {
        if (!defensePeriodRepository.existsById(defensePeriodId)) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND);
        }
        return PageResponse.from(
                committeeRepository.findByDefensePeriod(
                        defensePeriodId, status, normalize(keyword), PaginationSupport.pageRequest(page, size)),
                committeeMapper::toResponse);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public DefenseCommitteeResponse getById(Long committeeId) {
        return committeeMapper.toResponse(findCommittee(committeeId));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public DefenseCommitteeResponse update(Long committeeId, DefenseCommitteeRequest request) {
        DefenseCommitteesEntity committee = findCommittee(committeeId);
        requireStatus(committee, DefenseCommitteeStatusConstain.DRAFT);
        findOpenPeriod(committee.getDefensePeriod().getID_Defense());
        validateRequest(request);
        String name = request.getCommitteeName().trim();
        if (committeeRepository.existsDuplicateName(
                name, committee.getDefensePeriod().getID_Defense(), committeeId)) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_ALREADY_EXISTS);
        }
        committee.setComitteesName(name);
        committee.setDescription(normalize(request.getDescription()));
        return committeeMapper.toResponse(committeeRepository.save(committee));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public DefenseCommitteeValidationResponse validate(Long committeeId) {
        return buildValidation(findCommittee(committeeId));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public DefenseCommitteeResponse activate(Long committeeId) {
        DefenseCommitteesEntity committee = findCommittee(committeeId);
        requireStatus(committee, DefenseCommitteeStatusConstain.DRAFT);
        findOpenPeriod(committee.getDefensePeriod().getID_Defense());
        DefenseCommitteeValidationResponse validation = buildValidation(committee);
        if (!validation.isValid()) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_NOT_READY);
        }
        committee.setStatus(DefenseCommitteeStatusConstain.ACTIVE);
        committee.setActivatedAt(LocalDateTime.now());
        committee.setDeactivationReason(null);
        return committeeMapper.toResponse(committeeRepository.save(committee));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public DefenseCommitteeResponse moveToDraft(Long committeeId) {
        DefenseCommitteesEntity committee = findCommittee(committeeId);
        requireStatus(committee, DefenseCommitteeStatusConstain.ACTIVE);
        requireNoUsableSchedule(committee);
        committee.setStatus(DefenseCommitteeStatusConstain.DRAFT);
        committee.setActivatedAt(null);
        return committeeMapper.toResponse(committeeRepository.save(committee));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public DefenseCommitteeResponse deactivate(Long committeeId, DeactivateDefenseCommitteeRequest request) {
        DefenseCommitteesEntity committee = findCommittee(committeeId);
        if (committee.getStatus() == DefenseCommitteeStatusConstain.INACTIVE) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_OPERATION_NOT_ALLOWED);
        }
        if (request == null
                || request.getReason() == null
                || request.getReason().isBlank()) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_DEACTIVATION_REASON_NOT_BLANK);
        }
        requireNoUsableSchedule(committee);
        committee.setStatus(DefenseCommitteeStatusConstain.INACTIVE);
        committee.setDeactivationReason(request.getReason().trim());
        return committeeMapper.toResponse(committeeRepository.save(committee));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public void delete(Long committeeId) {
        DefenseCommitteesEntity committee = findCommittee(committeeId);
        requireStatus(committee, DefenseCommitteeStatusConstain.DRAFT);
        if (!committee.getComitteesMember().isEmpty()
                || !committee.getDefenseSchedules().isEmpty()) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_IN_USE);
        }
        committeeRepository.delete(committee);
    }

    private DefenseCommitteeValidationResponse buildValidation(DefenseCommitteesEntity committee) {
        List<ComitteesMemberEntity> activeMembers = committee.getComitteesMember().stream()
                .filter(member -> member.getStatus() == CommitteeMemberStatusConstain.ACTIVE)
                .toList();
        long chairpersons = countRole(activeMembers, CommitteeMemberRoleConstain.CHAIRPERSON);
        long secretaries = countRole(activeMembers, CommitteeMemberRoleConstain.SECRETARY);
        long reviewers = countRole(activeMembers, CommitteeMemberRoleConstain.REVIEWER);
        List<String> errors = new ArrayList<>();
        if (chairpersons != 1) {
            errors.add("Committee must have exactly one active chairperson");
        }
        if (secretaries != 1) {
            errors.add("Committee must have exactly one active secretary");
        }
        if (reviewers < 1) {
            errors.add("Committee must have at least one active reviewer");
        }
        if (activeMembers.size() < MINIMUM_ACTIVE_MEMBERS) {
            errors.add("Committee must have at least three active members");
        }
        if (activeMembers.stream()
                .anyMatch(member -> member.getLecture() == null
                        || member.getLecture().getUser() == null
                        || member.getLecture().getUser().getStatus() != StatusConstain.ACTIVE)) {
            errors.add("Every active committee member must have an active lecturer account");
        }
        return DefenseCommitteeValidationResponse.builder()
                .valid(errors.isEmpty())
                .activeMemberCount(activeMembers.size())
                .chairpersonCount(chairpersons)
                .secretaryCount(secretaries)
                .reviewerCount(reviewers)
                .errors(errors)
                .build();
    }

    private long countRole(List<ComitteesMemberEntity> members, CommitteeMemberRoleConstain role) {
        return members.stream().filter(member -> member.getRole() == role).count();
    }

    private void requireNoUsableSchedule(DefenseCommitteesEntity committee) {
        boolean inUse = committee.getDefenseSchedules().stream()
                .anyMatch(schedule -> schedule.getStatus() != DefenseScheduleStatusConstain.CANCELLED);
        if (inUse) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_IN_USE);
        }
    }

    private DefensePeriodEntity findOpenPeriod(Long defensePeriodId) {
        DefensePeriodEntity period = defensePeriodRepository
                .findById(defensePeriodId)
                .orElseThrow(() -> new AppException(ErrorCode.DEFENSE_PERIOD_NOT_FOUND));
        if (period.getStatus() == DefensePeriodConstain.FINISHED) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_FINISHED);
        }
        return period;
    }

    private DefenseCommitteesEntity findCommittee(Long committeeId) {
        return committeeRepository
                .findById(committeeId)
                .orElseThrow(() -> new AppException(ErrorCode.DEFENSE_COMMITTEE_NOT_FOUND));
    }

    private void requireStatus(DefenseCommitteesEntity committee, DefenseCommitteeStatusConstain expected) {
        if (committee.getStatus() != expected) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_OPERATION_NOT_ALLOWED);
        }
    }

    private void validateRequest(DefenseCommitteeRequest request) {
        if (request == null
                || request.getCommitteeName() == null
                || request.getCommitteeName().isBlank()) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_NAME_NOT_BLANK);
        }
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

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

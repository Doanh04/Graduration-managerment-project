package com.graduration.Service.ManagerService;

import java.time.LocalDateTime;

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
import com.graduration.Constain.StatusConstain;
import com.graduration.DTO.Request.AssignCommitteeMemberRequest;
import com.graduration.DTO.Request.DeactivateCommitteeMemberRequest;
import com.graduration.DTO.Request.UpdateCommitteeMemberRequest;
import com.graduration.DTO.Response.CommitteeMemberResponse;
import com.graduration.DTO.Response.PageResponse;
import com.graduration.Repository.CommitteeMemberRepository;
import com.graduration.Repository.DefenseCommitteeRepository;
import com.graduration.Repository.LectureRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.entity.ComitteesMemberEntity;
import com.graduration.entity.DefenseCommitteesEntity;
import com.graduration.entity.LectureEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.CommitteeMemberMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommitteeMemberService {
    CommitteeMemberRepository memberRepository;
    DefenseCommitteeRepository committeeRepository;
    LectureRepository lectureRepository;
    UserRepository userRepository;
    CommitteeMemberMapper memberMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public CommitteeMemberResponse assign(Long committeeId, AssignCommitteeMemberRequest request) {
        DefenseCommitteesEntity committee = findMutableCommittee(committeeId);
        LectureEntity lecture = lectureRepository
                .findById(request.getLectureId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        requireActiveLecturer(lecture);
        if (memberRepository.existsByDefenseCommittees_IdComitteesAndLecture_LectureIdAndStatus(
                committeeId, lecture.getLectureId(), CommitteeMemberStatusConstain.ACTIVE)) {
            throw new AppException(ErrorCode.COMMITTEE_MEMBER_ALREADY_ASSIGNED);
        }
        requireUniqueRole(committeeId, request.getRole(), null);
        ComitteesMemberEntity member = ComitteesMemberEntity.builder()
                .defenseCommittees(committee)
                .lecture(lecture)
                .role(request.getRole())
                .status(CommitteeMemberStatusConstain.ACTIVE)
                .assignedAt(LocalDateTime.now())
                .assignedBy(currentUser())
                .note(normalize(request.getNote()))
                .build();
        return memberMapper.toResponse(memberRepository.save(member));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public PageResponse<CommitteeMemberResponse> getByCommittee(Long committeeId, Integer page, Integer size) {
        if (!committeeRepository.existsById(committeeId)) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_NOT_FOUND);
        }
        return PageResponse.from(
                memberRepository.findByDefenseCommittees_IdComittees(
                        committeeId, PaginationSupport.pageRequest(page, size)),
                memberMapper::toResponse);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional(readOnly = true)
    public PageResponse<CommitteeMemberResponse> getByLecturer(String lectureId, Integer page, Integer size) {
        if (!lectureRepository.existsById(lectureId)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return activeByLecturer(lectureId, page, size);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY', 'ROLE_REVIEWER', 'ROLE_SUPERVISOR')")
    @Transactional(readOnly = true)
    public PageResponse<CommitteeMemberResponse> getMine(Integer page, Integer size) {
        LectureEntity lecture = lectureRepository
                .findByUser_UserId(currentAuthentication().getName())
                .orElseThrow(() -> new AppException(ErrorCode.LECTURER_PROFILE_NOT_FOUND));
        return activeByLecturer(lecture.getLectureId(), page, size);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public CommitteeMemberResponse update(Long memberId, UpdateCommitteeMemberRequest request) {
        ComitteesMemberEntity member = findMember(memberId);
        requireActive(member);
        findMutableCommittee(member.getDefenseCommittees().getIdComittees());
        requireUniqueRole(member.getDefenseCommittees().getIdComittees(), request.getRole(), memberId);
        member.setRole(request.getRole());
        member.setNote(normalize(request.getNote()));
        return memberMapper.toResponse(memberRepository.save(member));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public CommitteeMemberResponse deactivate(Long memberId, DeactivateCommitteeMemberRequest request) {
        ComitteesMemberEntity member = findMember(memberId);
        requireActive(member);
        findMutableCommittee(member.getDefenseCommittees().getIdComittees());
        if (request == null
                || request.getReason() == null
                || request.getReason().isBlank()) {
            throw new AppException(ErrorCode.COMMITTEE_MEMBER_DEACTIVATION_REASON_NOT_BLANK);
        }
        member.setStatus(CommitteeMemberStatusConstain.INACTIVE);
        member.setEndedAt(LocalDateTime.now());
        member.setNote(appendReason(member.getNote(), request.getReason()));
        return memberMapper.toResponse(memberRepository.save(member));
    }

    private PageResponse<CommitteeMemberResponse> activeByLecturer(String lectureId, Integer page, Integer size) {
        return PageResponse.from(
                memberRepository.findByLecture_LectureIdAndStatus(
                        lectureId, CommitteeMemberStatusConstain.ACTIVE, PaginationSupport.pageRequest(page, size)),
                memberMapper::toResponse);
    }

    private DefenseCommitteesEntity findMutableCommittee(Long committeeId) {
        DefenseCommitteesEntity committee = committeeRepository
                .findById(committeeId)
                .orElseThrow(() -> new AppException(ErrorCode.DEFENSE_COMMITTEE_NOT_FOUND));
        if (committee.getDefensePeriod() == null) {
            throw new AppException(ErrorCode.COMMITTEE_PERIOD_MISMATCH);
        }
        if (committee.getStatus() != DefenseCommitteeStatusConstain.DRAFT) {
            throw new AppException(ErrorCode.DEFENSE_COMMITTEE_OPERATION_NOT_ALLOWED);
        }
        if (committee.getDefensePeriod().getStatus() == DefensePeriodConstain.FINISHED) {
            throw new AppException(ErrorCode.DEFENSE_PERIOD_FINISHED);
        }
        return committee;
    }

    private void requireActiveLecturer(LectureEntity lecture) {
        StatusConstain status =
                lecture.getUser() == null ? null : lecture.getUser().getStatus();
        if (lecture.getUser() == null || status == StatusConstain.INACTIVE || status == StatusConstain.DELETED) {
            throw new AppException(ErrorCode.LECTURER_INACTIVE);
        }
    }

    private void requireUniqueRole(Long committeeId, CommitteeMemberRoleConstain role, Long excludedMemberId) {
        if (role != CommitteeMemberRoleConstain.CHAIRPERSON && role != CommitteeMemberRoleConstain.SECRETARY) {
            return;
        }
        boolean exists = excludedMemberId == null
                ? memberRepository.existsByDefenseCommittees_IdComitteesAndRoleAndStatus(
                        committeeId, role, CommitteeMemberStatusConstain.ACTIVE)
                : memberRepository.existsByDefenseCommittees_IdComitteesAndRoleAndStatusAndComitteesMemberIdNot(
                        committeeId, role, CommitteeMemberStatusConstain.ACTIVE, excludedMemberId);
        if (exists) {
            throw new AppException(
                    role == CommitteeMemberRoleConstain.CHAIRPERSON
                            ? ErrorCode.COMMITTEE_CHAIRPERSON_ALREADY_EXISTS
                            : ErrorCode.COMMITTEE_SECRETARY_ALREADY_EXISTS);
        }
    }

    private ComitteesMemberEntity findMember(Long memberId) {
        return memberRepository
                .findById(memberId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMITTEE_MEMBER_NOT_FOUND));
    }

    private void requireActive(ComitteesMemberEntity member) {
        if (member.getStatus() != CommitteeMemberStatusConstain.ACTIVE) {
            throw new AppException(ErrorCode.COMMITTEE_MEMBER_NOT_ACTIVE);
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

    private String appendReason(String note, String reason) {
        String normalizedReason = reason.trim();
        return note == null || note.isBlank()
                ? "Deactivated: " + normalizedReason
                : note.trim() + " | Deactivated: " + normalizedReason;
    }
}

package com.graduration.mapper;

import org.springframework.stereotype.Component;

import com.graduration.Constain.CommitteeMemberStatusConstain;
import com.graduration.DTO.Response.DefenseCommitteeResponse;
import com.graduration.entity.DefenseCommitteesEntity;

@Component
public class DefenseCommitteeMapper {
    public DefenseCommitteeResponse toResponse(DefenseCommitteesEntity entity) {
        long activeMembers = entity.getComitteesMember().stream()
                .filter(member -> member.getStatus() == CommitteeMemberStatusConstain.ACTIVE)
                .count();
        return DefenseCommitteeResponse.builder()
                .committeeId(entity.getIdComittees())
                .committeeName(entity.getComitteesName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .defensePeriodId(
                        entity.getDefensePeriod() == null
                                ? null
                                : entity.getDefensePeriod().getID_Defense())
                .defensePeriodName(
                        entity.getDefensePeriod() == null
                                ? null
                                : entity.getDefensePeriod().getPeriodName())
                .academicYearId(
                        entity.getAcademicYear() == null
                                ? null
                                : entity.getAcademicYear().getAcademicId())
                .academicYear(
                        entity.getAcademicYear() == null
                                ? null
                                : entity.getAcademicYear().getAcademicYear())
                .activeMemberCount(activeMembers)
                .scheduleCount(entity.getDefenseSchedules().size())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .activatedAt(entity.getActivatedAt())
                .deactivationReason(entity.getDeactivationReason())
                .createdByUserId(
                        entity.getCreatedBy() == null
                                ? null
                                : entity.getCreatedBy().getUserId())
                .createdByUsername(
                        entity.getCreatedBy() == null
                                ? null
                                : entity.getCreatedBy().getUserName())
                .build();
    }
}

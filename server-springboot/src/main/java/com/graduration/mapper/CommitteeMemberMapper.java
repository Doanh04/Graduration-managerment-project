package com.graduration.mapper;

import org.springframework.stereotype.Component;

import com.graduration.DTO.Response.CommitteeMemberResponse;
import com.graduration.entity.ComitteesMemberEntity;

@Component
public class CommitteeMemberMapper {
    public CommitteeMemberResponse toResponse(ComitteesMemberEntity entity) {
        return CommitteeMemberResponse.builder()
                .memberId(entity.getComitteesMemberId())
                .committeeId(entity.getDefenseCommittees().getIdComittees())
                .committeeName(entity.getDefenseCommittees().getComitteesName())
                .defensePeriodId(
                        entity.getDefenseCommittees().getDefensePeriod() == null
                                ? null
                                : entity.getDefenseCommittees()
                                        .getDefensePeriod()
                                        .getID_Defense())
                .lectureId(entity.getLecture().getLectureId())
                .lectureCode(entity.getLecture().getLectureCode())
                .lectureName(entity.getLecture().getFullNameLecture())
                .role(entity.getRole())
                .status(entity.getStatus())
                .assignedAt(entity.getAssignedAt())
                .endedAt(entity.getEndedAt())
                .assignedByUserId(
                        entity.getAssignedBy() == null
                                ? null
                                : entity.getAssignedBy().getUserId())
                .assignedByUsername(
                        entity.getAssignedBy() == null
                                ? null
                                : entity.getAssignedBy().getUserName())
                .note(entity.getNote())
                .build();
    }
}

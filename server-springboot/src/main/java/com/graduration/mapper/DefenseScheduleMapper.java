package com.graduration.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.graduration.Constain.CommitteeMemberStatusConstain;
import com.graduration.DTO.Response.DefenseScheduleResponse;
import com.graduration.entity.DefenseSchedulesEntity;

@Component
public class DefenseScheduleMapper {
    public DefenseScheduleResponse toResponse(DefenseSchedulesEntity entity) {
        List<DefenseScheduleResponse.StudentSummary> students =
                entity.getTopic().getTeam() == null
                        ? List.of()
                        : entity.getTopic().getTeam().getStudentEntities().stream()
                                .map(student -> DefenseScheduleResponse.StudentSummary.builder()
                                        .studentId(student.getIdStudent())
                                        .studentCode(student.getStudentCode())
                                        .fullName(student.getFullNameStudent())
                                        .build())
                                .toList();
        List<DefenseScheduleResponse.MemberSummary> members =
                entity.getDefenseCommittees().getComitteesMember().stream()
                        .filter(member -> member.getStatus() == CommitteeMemberStatusConstain.ACTIVE)
                        .map(member -> DefenseScheduleResponse.MemberSummary.builder()
                                .lectureId(member.getLecture().getLectureId())
                                .lectureCode(member.getLecture().getLectureCode())
                                .lectureName(member.getLecture().getFullNameLecture())
                                .role(member.getRole())
                                .build())
                        .toList();
        return DefenseScheduleResponse.builder()
                .scheduleId(entity.getIdDefenseScheduce())
                .defensePeriodId(entity.getTopic().getDefensePeriod().getID_Defense())
                .defenseDate(entity.getDefenseDate())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .room(entity.getRoom())
                .location(entity.getLocation())
                .session(entity.getSession())
                .status(entity.getStatus())
                .note(entity.getNote())
                .topicId(entity.getTopic().getIdTopic())
                .topicTitle(entity.getTopic().getTitle())
                .teamId(
                        entity.getTopic().getTeam() == null
                                ? null
                                : entity.getTopic().getTeam().getIdTeam())
                .teamName(
                        entity.getTopic().getTeam() == null
                                ? null
                                : entity.getTopic().getTeam().getNameTeam())
                .students(students)
                .committeeId(entity.getDefenseCommittees().getIdComittees())
                .committeeName(entity.getDefenseCommittees().getComitteesName())
                .committeeMembers(members)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .publishedAt(entity.getPublishedAt())
                .postponedAt(entity.getPostponedAt())
                .postponedReason(entity.getPostponedReason())
                .cancelledAt(entity.getCancelledAt())
                .cancelledReason(entity.getCancelledReason())
                .createdByUserId(
                        entity.getCreatedBy() == null
                                ? null
                                : entity.getCreatedBy().getUserId())
                .build();
    }
}

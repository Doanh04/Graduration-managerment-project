package com.graduration.DTO.Response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.graduration.Constain.CommitteeMemberRoleConstain;
import com.graduration.Constain.DefenseScheduleStatusConstain;
import com.graduration.Constain.DefenseSessionConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DefenseScheduleResponse {
    Long scheduleId;
    Long defensePeriodId;
    LocalDate defenseDate;
    LocalTime startTime;
    LocalTime endTime;
    String room;
    String location;
    DefenseSessionConstain session;
    DefenseScheduleStatusConstain status;
    String note;
    Long topicId;
    String topicTitle;
    Long teamId;
    String teamName;
    List<StudentSummary> students;
    Long committeeId;
    String committeeName;
    List<MemberSummary> committeeMembers;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime publishedAt;
    LocalDateTime postponedAt;
    String postponedReason;
    LocalDateTime cancelledAt;
    String cancelledReason;
    String createdByUserId;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentSummary {
        String studentId;
        String studentCode;
        String fullName;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberSummary {
        String lectureId;
        String lectureCode;
        String lectureName;
        CommitteeMemberRoleConstain role;
    }
}

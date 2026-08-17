package com.graduration.DTO.Response;

import java.time.LocalDateTime;

import com.graduration.Constain.CommitteeMemberRoleConstain;
import com.graduration.Constain.CommitteeMemberStatusConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommitteeMemberResponse {
    Long memberId;
    Long committeeId;
    String committeeName;
    Long defensePeriodId;
    String lectureId;
    String lectureCode;
    String lectureName;
    CommitteeMemberRoleConstain role;
    CommitteeMemberStatusConstain status;
    LocalDateTime assignedAt;
    LocalDateTime endedAt;
    String assignedByUserId;
    String assignedByUsername;
    String note;
}

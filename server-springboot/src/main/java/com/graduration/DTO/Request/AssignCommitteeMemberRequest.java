package com.graduration.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.graduration.Constain.CommitteeMemberRoleConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssignCommitteeMemberRequest {
    @NotBlank(message = "LECTURER_NOT_BLANK")
    String lectureId;

    @NotNull(message = "COMMITTEE_MEMBER_ROLE_NOT_BLANK")
    CommitteeMemberRoleConstain role;

    String note;
}

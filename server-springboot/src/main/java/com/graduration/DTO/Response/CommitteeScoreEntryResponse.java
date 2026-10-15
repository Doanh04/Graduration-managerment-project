package com.graduration.DTO.Response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.graduration.Constain.CommitteeMemberRoleConstain;
import com.graduration.Constain.ScoreStatusConstain;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitteeScoreEntryResponse {
    private Long scoreId;
    private String lecturerId;
    private String lecturerCode;
    private String lecturerName;
    private CommitteeMemberRoleConstain role;
    private BigDecimal score;
    private String comment;
    private ScoreStatusConstain status;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;
}

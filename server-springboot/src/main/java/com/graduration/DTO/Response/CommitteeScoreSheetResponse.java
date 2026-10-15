package com.graduration.DTO.Response;

import java.math.BigDecimal;
import java.util.List;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitteeScoreSheetResponse {
    private String studentId;
    private String studentCode;
    private String studentName;
    private Long topicId;
    private String topicTitle;
    private Long committeeId;
    private String committeeName;
    private BigDecimal finalScore;
    private int scoredMembers;
    private int totalMembers;
    private boolean complete;
    private boolean published;
    private List<CommitteeScoreEntryResponse> entries;
}

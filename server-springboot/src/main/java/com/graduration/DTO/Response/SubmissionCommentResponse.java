package com.graduration.DTO.Response;

import java.time.LocalDateTime;

import com.graduration.Constain.CommentTypeConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmissionCommentResponse {
    Long commentId;
    Long submissionId;
    String content;
    CommentTypeConstain commentType;
    String lecturerId;
    String lecturerName;
    String authorUserId;
    String authorUsername;
    Boolean edited;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

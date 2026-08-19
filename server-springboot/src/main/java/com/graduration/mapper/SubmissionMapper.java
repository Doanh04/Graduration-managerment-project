package com.graduration.mapper;

import org.springframework.stereotype.Component;

import com.graduration.DTO.Response.SubmissionCommentResponse;
import com.graduration.DTO.Response.SubmissionResponse;
import com.graduration.entity.CommentEntity;
import com.graduration.entity.LectureEntity;
import com.graduration.entity.MilesStoneEntity;
import com.graduration.entity.StudentEntity;
import com.graduration.entity.SubmistionEntity;
import com.graduration.entity.TeamEntity;
import com.graduration.entity.UserEntity;

@Component
public class SubmissionMapper {
    public SubmissionResponse toResponse(SubmistionEntity submission) {
        MilesStoneEntity milestone = submission.getMilesStone();
        TeamEntity team = submission.getTeam();
        StudentEntity student = submission.getSubmittedBy();
        return SubmissionResponse.builder()
                .submissionId(submission.getIdSubmission())
                .milestoneId(milestone == null ? null : milestone.getIdMilesStone())
                .milestoneName(milestone == null ? null : milestone.getMilesStoneName())
                .teamId(team == null ? null : team.getIdTeam())
                .teamName(team == null ? null : team.getNameTeam())
                .studentId(student == null ? null : student.getIdStudent())
                .studentCode(student == null ? null : student.getStudentCode())
                .submittedByName(student == null ? null : student.getFullNameStudent())
                .fileName(submission.getFileName())
                .contentType(submission.getContentType())
                .fileSize(submission.getFileSize())
                .checksum(submission.getChecksum())
                .late(submission.getIsLate())
                .note(submission.getNote())
                .submittedAt(submission.getSubmittedAt())
                .updatedAt(submission.getUpdatedAt())
                .version(submission.getVersion())
                .status(submission.getStatus())
                .build();
    }

    public SubmissionCommentResponse toCommentResponse(CommentEntity comment) {
        LectureEntity lecturer = comment.getLecture();
        UserEntity author = comment.getCreatedBy();
        return SubmissionCommentResponse.builder()
                .commentId(comment.getIdComment())
                .submissionId(
                        comment.getSubmistion() == null
                                ? null
                                : comment.getSubmistion().getIdSubmission())
                .content(comment.getContent())
                .commentType(comment.getCommentType())
                .lecturerId(lecturer == null ? null : lecturer.getLectureId())
                .lecturerName(lecturer == null ? null : lecturer.getFullNameLecture())
                .authorUserId(author == null ? null : author.getUserId())
                .authorUsername(author == null ? null : author.getUserName())
                .edited(comment.getEdited())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}

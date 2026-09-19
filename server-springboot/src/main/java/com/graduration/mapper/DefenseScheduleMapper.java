package com.graduration.mapper;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.graduration.Constain.CommitteeMemberStatusConstain;
import com.graduration.Constain.EnrollmentStatusConstain;
import com.graduration.DTO.Response.DefenseScheduleResponse;
import com.graduration.entity.DefenseCommitteesEntity;
import com.graduration.entity.DefenseSchedulesEntity;
import com.graduration.entity.SubmistionEntity;
import com.graduration.entity.TeamEntity;
import com.graduration.entity.TopicEntity;
import com.graduration.Repository.GraduationEnrollmentRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DefenseScheduleMapper {
    private final GraduationEnrollmentRepository enrollmentRepository;

    public DefenseScheduleResponse toResponse(DefenseSchedulesEntity entity) {
        TopicEntity topic = entity.getTopic();
        // Lịch bảo vệ chỉ dùng nhóm đã được gán đề tài chính thức.
        TeamEntity team = selectTeam(topic);
        DefenseCommitteesEntity committee = entity.getDefenseCommittees();
        List<DefenseScheduleResponse.StudentSummary> students = team == null || team.getStudentEntities() == null
                ? List.of()
                : team.getStudentEntities().stream()
                        .filter(student -> student != null)
                        .map(student -> {
                            var enrollment = topic == null || topic.getDefensePeriod() == null
                                    ? null
                                    : enrollmentRepository
                                            .findByStudent_IdStudentAndDefensePeriod_ID_Defense(
                                                    student.getIdStudent(), topic.getDefensePeriod().getID_Defense())
                                            .orElse(null);
                            var enrollmentStatus = enrollment == null ? null : enrollment.getStatus();
                            return DefenseScheduleResponse.StudentSummary.builder()
                                    .studentId(student.getIdStudent())
                                    .studentCode(student.getStudentCode())
                                    .fullName(student.getFullNameStudent())
                                    .email(student.getEmail())
                                    .phone(student.getPhoneStudent())
                                    .enrollmentStatus(enrollmentStatus)
                                    .defenseEligible(enrollmentStatus != EnrollmentStatusConstain.WITHDRAWN)
                                    .build();
                        })
                        .toList();
        List<DefenseScheduleResponse.MemberSummary> members =
                committee == null || committee.getComitteesMember() == null
                        ? List.of()
                        : committee.getComitteesMember().stream()
                                .filter(member -> member != null
                                        && (member.getStatus() == CommitteeMemberStatusConstain.ACTIVE
                                                || member.getStatus() == null)
                                        && member.getLecture() != null)
                                .map(member -> DefenseScheduleResponse.MemberSummary.builder()
                                        .lectureId(member.getLecture().getLectureId())
                                        .lectureCode(member.getLecture().getLectureCode())
                                        .lectureName(member.getLecture().getFullNameLecture())
                                        .email(member.getLecture().getEmaillecture())
                                        .degree(member.getLecture().getDegree())
                                        .role(member.getRole())
                                        .build())
                                .toList();
        TopicFileMetadata topicFile =
                decodeTopicFile(topic == null ? null : topic.getFileData()).orElse(null);
        return DefenseScheduleResponse.builder()
                .scheduleId(entity.getIdDefenseScheduce())
                .defensePeriodId(
                        topic == null || topic.getDefensePeriod() == null
                                ? null
                                : topic.getDefensePeriod().getID_Defense())
                .defensePeriodName(
                        topic == null || topic.getDefensePeriod() == null
                                ? null
                                : topic.getDefensePeriod().getPeriodName())
                .academicYear(
                        topic == null
                                        || topic.getDefensePeriod() == null
                                        || topic.getDefensePeriod().getAcademicYear() == null
                                ? null
                                : topic.getDefensePeriod().getAcademicYear().getAcademicYear())
                .projectType(
                        topic == null || topic.getDefensePeriod() == null
                                ? null
                                : topic.getDefensePeriod().getProjectType())
                .defenseDate(entity.getDefenseDate())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .room(entity.getRoom())
                .location(entity.getLocation())
                .session(entity.getSession())
                .status(entity.getStatus())
                .note(entity.getNote())
                .topicId(topic == null ? null : topic.getIdTopic())
                .topicTitle(topic == null ? null : topic.getTitle())
                .topicDescription(topic == null ? null : topic.getDescription())
                .topicObjective(topic == null ? null : topic.getObjective())
                .topicTechnology(topic == null ? null : topic.getTechnology())
                .topicFileName(topicFile == null ? null : topicFile.fileName())
                .topicFileContentType(topicFile == null ? null : topicFile.contentType())
                .topicFileSize(topicFile == null ? null : topicFile.fileSize())
                .teamId(team == null ? null : team.getIdTeam())
                .teamName(team == null ? null : team.getNameTeam())
                .teamDescription(team == null ? null : team.getDescription())
                .submissionId(latestSubmissionId(team))
                .students(students)
                .committeeId(committee == null ? null : committee.getIdComittees())
                .committeeName(committee == null ? null : committee.getComitteesName())
                .committeeDescription(committee == null ? null : committee.getDescription())
                .committeeStatus(committee == null ? null : committee.getStatus())
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

    /**
     * Đọc phần header metadata của file_data để đưa tên, MIME và kích thước tệp
     * đề tài vào response lịch; nội dung nhị phân không được trả về trong JSON.
     */
    private Optional<TopicFileMetadata> decodeTopicFile(byte[] payload) {
        if (payload == null || payload.length < 16) {
            return Optional.empty();
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            if (input.readInt() != 0x54465031) { // TFP1
                return Optional.empty();
            }
            String fileName = readText(input, 1024);
            String contentType = readText(input, 255);
            int fileSize = input.readInt();
            if (fileSize < 0 || fileSize > 10 * 1024 * 1024 || fileSize > input.available()) {
                return Optional.empty();
            }
            return Optional.of(new TopicFileMetadata(fileName, contentType, (long) fileSize));
        } catch (IOException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    private String readText(DataInputStream input, int maxLength) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > maxLength || length > input.available()) {
            throw new IOException("Invalid file metadata");
        }
        return new String(input.readNBytes(length), java.nio.charset.StandardCharsets.UTF_8);
    }

    private record TopicFileMetadata(String fileName, String contentType, long fileSize) {}

    /**
     * Chọn nhóm thực hiện đã được gán chính thức để hiển thị lịch bảo vệ.
     */
    private TeamEntity selectTeam(TopicEntity topic) {
        if (topic == null) {
            return null;
        }
        return topic.getTeam();
    }

    /**
     * Chọn bài nộp mới nhất của nhóm để các nhận xét trong luồng bảo vệ được
     * gắn vào đúng hồ sơ hiện hành mà sinh viên và giảng viên đang theo dõi.
     */
    private Long latestSubmissionId(TeamEntity team) {
        if (team == null || team.getSubmistion() == null) {
            return null;
        }
        SubmistionEntity latest = null;
        for (SubmistionEntity submission : team.getSubmistion()) {
            if (submission == null || submission.getIdSubmission() == null) {
                continue;
            }
            if (latest == null || compareSubmissionVersion(submission, latest) > 0) {
                latest = submission;
            }
        }
        return latest == null ? null : latest.getIdSubmission();
    }

    private int compareSubmissionVersion(SubmistionEntity left, SubmistionEntity right) {
        int version = Integer.compare(
                left.getVersion() == null ? 0 : left.getVersion(), right.getVersion() == null ? 0 : right.getVersion());
        if (version != 0) {
            return version;
        }
        if (left.getSubmittedAt() == null) {
            return right.getSubmittedAt() == null ? 0 : -1;
        }
        if (right.getSubmittedAt() == null) {
            return 1;
        }
        return left.getSubmittedAt().compareTo(right.getSubmittedAt());
    }
}

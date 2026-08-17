package com.graduration.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import com.graduration.Constain.SubmissionStatusConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "Submistion",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_submission_team_milestone_version",
                        columnNames = {"id_team", "Id_Miles_Stone", "version"}))
// ưu trữ thông tin mỗi khi sinh viên/nhóm sinh viên tải lên (upload) file báo cáo, sản phẩm cho từng mốc tiến độ.
public class SubmistionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID_Submission")
    Long IdSubmission;

    @Column(name = "File_Path", nullable = false) // đường dẫn lưu trữ file trên hệ thống
    String filePath;

    @Column(name = "File_Name", nullable = false) // Tên file
    String fileName;

    @Column(name = "stored_file_name")
    String storedFileName;

    @Column(name = "content_type")
    String contentType;

    @Column(name = "file_size")
    Long fileSize;

    @Column(name = "checksum")
    String checksum;

    @Column(name = "Is_Late", nullable = false) // xacs định nộp muộn không
    Boolean isLate;

    @Column(name = "note")
    String note; // ghi chú của sinh viên về bài nộp

    @Column(name = "submitted_at")
    LocalDateTime submittedAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Column(name = "version")
    Integer version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    SubmissionStatusConstain status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by", nullable = false)
    StudentEntity submittedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Id_Miles_Stone", nullable = false)
    MilesStoneEntity milesStone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_team", nullable = false)
    TeamEntity team;

    @OneToMany(mappedBy = "submistion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<CommentEntity> comment = new ArrayList<>();

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        submittedAt = submittedAt == null ? now : submittedAt;
        updatedAt = now;
        version = version == null ? 1 : version;
        status = status == null ? SubmissionStatusConstain.SUBMITTED : status;
        isLate = isLate != null && isLate;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

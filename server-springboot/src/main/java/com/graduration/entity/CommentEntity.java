package com.graduration.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.graduration.Constain.CommentTypeConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "comment")
public class CommentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_comment")
    Long idComment;

    @Column(name = "Content", nullable = false)
    String content; // Nội dung lời nhận xét, góp ý hoặc sửa lỗi của giảng viên

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_type")
    CommentTypeConstain commentType;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Column(name = "edited", nullable = false)
    @Builder.Default
    Boolean edited = false;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_Submission")
    SubmistionEntity submistion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id")
    LectureEntity lecture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    UserEntity createdBy;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = now;
        edited = edited != null && edited;
        commentType = commentType == null ? CommentTypeConstain.COMMENT : commentType;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

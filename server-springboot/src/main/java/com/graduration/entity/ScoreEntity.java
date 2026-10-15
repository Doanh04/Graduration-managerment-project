package com.graduration.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.graduration.Constain.ScoreStatusConstain;
import com.graduration.Constain.ScoreTypeConstain;

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
        name = "score",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_score_student_topic_lecturer_type",
                        columnNames = {"id_student", "id_topic", "lecture_id", "score_type"}),
                @UniqueConstraint(name = "uk_score_automatic_schedule_student",
                        columnNames = {"automatic_schedule_id", "id_student"}),
                @UniqueConstraint(name = "uk_score_automatic_review_student",
                        columnNames = {"automatic_review_assignment_id", "id_student"}),
                @UniqueConstraint(name = "uk_score_automatic_final_student",
                        columnNames = {"automatic_final_topic_id", "id_student"})})
public class ScoreEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "score_id")
    Long id;

    @Column(name = "score", precision = 5, scale = 2, nullable = false)
    BigDecimal score;

    @Column(name = "comment", length = 4000)
    String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_type")
    ScoreTypeConstain scoreType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    ScoreStatusConstain status;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // Automatic deadline results have no human evaluator.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id")
    LectureEntity lecture;

    Long automaticScheduleId;
    Long automaticReviewAssignmentId;
    @Column(name = "automatic_final_topic_id")
    Long automaticFinalTopicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_student", nullable = false)
    StudentEntity student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_topic", nullable = false)
    TopicEntity topic;

    @Column(name = "submitted_at")
    LocalDateTime submittedAt;

    @Column(name = "published_at")
    LocalDateTime publishedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = now;
        status = status == null ? ScoreStatusConstain.DRAFT : status;
        scoreType = scoreType == null ? ScoreTypeConstain.FINAL : scoreType;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

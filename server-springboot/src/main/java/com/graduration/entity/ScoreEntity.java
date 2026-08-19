package com.graduration.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_score_student_topic",
                        columnNames = {"id_student", "id_topic"}))
public class ScoreEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "score_id")
    Long id;

    @Column(name = "score", precision = 5, scale = 2, nullable = false)
    BigDecimal score;

    @Column(name = "comment")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id")
    LectureEntity lecture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_team")
    TeamEntity team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_student")
    StudentEntity student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_topic")
    TopicEntity topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criterion_id")
    ScoreCriterionEntity criterion;

    @OneToMany(mappedBy = "score", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<ScoreDetailEntity> details = new ArrayList<>();

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

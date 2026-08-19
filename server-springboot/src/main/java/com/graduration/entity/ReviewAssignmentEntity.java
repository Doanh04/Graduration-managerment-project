package com.graduration.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.graduration.Constain.ReviewAssignmentStatusConstain;
import com.graduration.Constain.ReviewRecommendationConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "review_assignment") // phân cộng phản biện
public class ReviewAssignmentEntity {
    @Id
    @GeneratedValue
    @Column(name = "id_review")
    Long reviewAssignmentId;

    @Column(name = "asigned_date", nullable = false, updatable = false)
    LocalDateTime assignedAt;

    @Column(name = "deadline", nullable = false)
    LocalDateTime deadline;

    @Column(name = "submitted_at")
    LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    LocalDateTime reviewedAt;

    @Column(name = "cancelled_at")
    LocalDateTime cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    ReviewAssignmentStatusConstain status;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation")
    ReviewRecommendationConstain recommendation;

    @Column(name = "review_comment", length = 4000)
    String reviewComment;

    @Column(name = "note", length = 1000)
    String note;

    @Column(name = "cancelled_reason", length = 1000)
    String cancelledReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    UserEntity assignedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    UserEntity reviewedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_topic", nullable = false)
    TopicEntity topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    LectureEntity lecture;

    @PrePersist
    void prePersist() {
        assignedAt = assignedAt == null ? LocalDateTime.now() : assignedAt;
        status = status == null ? ReviewAssignmentStatusConstain.ASSIGNED : status;
    }
}

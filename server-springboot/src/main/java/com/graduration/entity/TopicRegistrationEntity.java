package com.graduration.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.graduration.Constain.TopicRegistrationStatusConstain;

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
        name = "topic_registration",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_topic_registration_enrollment_priority",
                    columnNames = {"enrollment_id", "priority"}),
            @UniqueConstraint(
                    name = "uk_topic_registration_enrollment_topic",
                    columnNames = {"enrollment_id", "id_topic"})
        })
public class TopicRegistrationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "registration_id")
    Long registrationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    GraduationEnrollmentEntity enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_topic", nullable = false)
    TopicEntity topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_supervisor_id")
    LectureEntity preferredSupervisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_team")
    TeamEntity team;

    @Column(name = "priority", nullable = false)
    Integer priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    TopicRegistrationStatusConstain status;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    UserEntity reviewedBy;

    @Column(name = "rejection_reason")
    String rejectionReason;

    @Column(name = "note")
    String note;

    @PrePersist
    void prePersist() {
        submittedAt = submittedAt == null ? LocalDateTime.now() : submittedAt;
        status = status == null ? TopicRegistrationStatusConstain.PENDING : status;
        priority = priority == null ? 1 : priority;
    }
}

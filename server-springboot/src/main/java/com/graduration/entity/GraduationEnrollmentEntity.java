package com.graduration.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import com.graduration.Constain.EnrollmentStatusConstain;

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
        name = "graduation_enrollment",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_enrollment_student_period",
                        columnNames = {"id_student", "id_defense"}))
public class GraduationEnrollmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "enrollment_id")
    Long enrollmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_student", nullable = false)
    StudentEntity student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_defense", nullable = false)
    DefensePeriodEntity defensePeriod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    EnrollmentStatusConstain status;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    LocalDateTime enrolledAt;

    @Column(name = "completed_at")
    LocalDateTime completedAt;

    @Column(name = "note")
    String note;

    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<TopicRegistrationEntity> topicRegistrations = new ArrayList<>();

    @PrePersist
    void prePersist() {
        enrolledAt = enrolledAt == null ? LocalDateTime.now() : enrolledAt;
        status = status == null ? EnrollmentStatusConstain.ENROLLED : status;
    }
}

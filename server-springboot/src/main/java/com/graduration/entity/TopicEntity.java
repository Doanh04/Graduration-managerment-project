package com.graduration.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import com.graduration.Constain.CategoryTopicConstain;
import com.graduration.Constain.TopicStatusConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "topic")
public class TopicEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_topic")
    Long idTopic;

    @Column(name = "title", nullable = false)
    String title;

    @Column(name = "description")
    String description;

    @Column(name = "objective")
    String objective; // mục tiêu của đề tài

    @Column(name = "technology")
    String technology;

    @Column(name = "category_topic")
    @Enumerated(EnumType.STRING)
    CategoryTopicConstain categoryTopic;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    TopicStatusConstain status;

    @Column(name = "created_by")
    String createdBy;

    @Column(name = "rejection_reason")
    String rejectionReason;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @OneToOne(mappedBy = "topic", cascade = CascadeType.ALL)
    TeamEntity team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_Defense", nullable = false)
    DefensePeriodEntity defensePeriod;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<TopicSuperVisorEntity> topicSuperVisorEntities = new ArrayList<>();

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<ReviewAssignmentEntity> reviewAssignment = new ArrayList<>();

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<TopicRegistrationEntity> topicRegistrations = new ArrayList<>();

    @OneToMany(mappedBy = "topic")
    @Builder.Default
    List<ScoreEntity> scores = new ArrayList<>();

    @OneToOne(mappedBy = "topic", cascade = CascadeType.ALL)
    DefenseSchedulesEntity defenseSchedule;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = now;
        status = status == null ? TopicStatusConstain.DRAFT : status;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

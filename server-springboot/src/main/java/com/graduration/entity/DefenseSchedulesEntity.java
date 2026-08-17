package com.graduration.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import com.graduration.Constain.DefenseScheduleStatusConstain;
import com.graduration.Constain.DefenseSessionConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "defense_schedules") // lịch bảo vệ của từng đề tài.
public class DefenseSchedulesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_defense_scheduce")
    Long idDefenseScheduce;

    @Column(name = "defense_date", nullable = false) // ngày bảo vệ
    LocalDate defenseDate;

    @Column(name = "room", nullable = false)
    String room;

    @Column(name = "location", nullable = false)
    String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "sesstion")
    DefenseSessionConstain session;

    @Column(name = "start_time")
    LocalTime startTime;

    @Column(name = "end_time")
    LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    DefenseScheduleStatusConstain status;

    @Column(name = "note")
    String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @Column(name = "published_at")
    LocalDateTime publishedAt;

    @Column(name = "postponed_at")
    LocalDateTime postponedAt;

    @Column(name = "postponed_reason", length = 1000)
    String postponedReason;

    @Column(name = "cancelled_at")
    LocalDateTime cancelledAt;

    @Column(name = "cancelled_reason", length = 1000)
    String cancelledReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    UserEntity createdBy;

    @OneToOne
    @JoinColumn(name = "id_topic", unique = true, nullable = false)
    TopicEntity topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_Comittees", nullable = false)
    DefenseCommitteesEntity defenseCommittees;

    @OneToMany(mappedBy = "schedule")
    @Builder.Default
    List<DefenseScheduleHistoryEntity> histories = new ArrayList<>();

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = now;
        status = status == null ? DefenseScheduleStatusConstain.DRAFT : status;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

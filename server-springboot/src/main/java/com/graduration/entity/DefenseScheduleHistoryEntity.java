package com.graduration.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.*;

import com.graduration.Constain.DefenseScheduleHistoryActionConstain;
import com.graduration.Constain.DefenseScheduleStatusConstain;

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
        name = "defense_schedule_history",
        indexes = {
            @Index(name = "idx_schedule_history_schedule_time", columnList = "schedule_id, changed_at"),
            @Index(name = "idx_schedule_history_changed_by", columnList = "changed_by"),
            @Index(name = "idx_schedule_history_action", columnList = "action")
        })
public class DefenseScheduleHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "history_id")
    Long historyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    DefenseSchedulesEntity schedule;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    DefenseScheduleHistoryActionConstain action;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    DefenseScheduleStatusConstain previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status")
    DefenseScheduleStatusConstain newStatus;

    LocalDate oldDefenseDate;
    LocalTime oldStartTime;
    LocalTime oldEndTime;
    String oldRoom;
    String oldLocation;
    Long oldCommitteeId;
    String oldCommitteeName;

    LocalDate newDefenseDate;
    LocalTime newStartTime;
    LocalTime newEndTime;
    String newRoom;
    String newLocation;
    Long newCommitteeId;
    String newCommitteeName;

    @Column(name = "reason", length = 1000)
    String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    UserEntity changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    LocalDateTime changedAt;

    @PrePersist
    void prePersist() {
        changedAt = changedAt == null ? LocalDateTime.now() : changedAt;
    }
}

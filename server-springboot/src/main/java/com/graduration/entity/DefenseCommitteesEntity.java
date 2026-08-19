package com.graduration.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import com.graduration.Constain.DefenseCommitteeStatusConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "defense_comittees") // thông tin về các hội đồng bảo vệ.
public class DefenseCommitteesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_Comittees")
    Long idComittees;

    @Column(name = "comittees_name", nullable = false)
    String comitteesName;

    @Column(name = "description", length = 1000)
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    DefenseCommitteeStatusConstain status;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @Column(name = "activated_at")
    LocalDateTime activatedAt;

    @Column(name = "deactivation_reason", length = 1000)
    String deactivationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_id", nullable = false)
    AcademicYearEntity academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_defense")
    DefensePeriodEntity defensePeriod;

    @OneToMany(mappedBy = "defenseCommittees", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<ComitteesMemberEntity> comitteesMember = new ArrayList<>();

    @OneToMany(mappedBy = "defenseCommittees", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<DefenseSchedulesEntity> defenseSchedules = new ArrayList<>();

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = now;
        status = status == null ? DefenseCommitteeStatusConstain.DRAFT : status;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

package com.graduration.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import com.graduration.Constain.DefensePeriodConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "Defense_period")
// Đợt bảo vệ đồ án
public class DefensePeriodEntity {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "Id_Defense")
    Long ID_Defense;

    @Column(name = "period_name", nullable = false)
    String periodName;

    @Column(name = "start_date", nullable = false)
    LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    LocalDate endDate;

    @Column(name = "final_submission_deadline")
    LocalDateTime finalSubmissionDeadline;

    @Column(name = "project_type")
    String projectType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    DefensePeriodConstain status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_id", nullable = false)
    AcademicYearEntity academicYear;

    @OneToMany(mappedBy = "defensePeriod", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<TopicEntity> topic = new ArrayList<>();

    @OneToMany(mappedBy = "defensePeriod", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<MilesStoneEntity> milesStone = new ArrayList<>();

    @OneToMany(mappedBy = "defensePeriod", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<GraduationEnrollmentEntity> graduationEnrollments = new ArrayList<>();

    @OneToMany(mappedBy = "defensePeriod", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<DefenseCommitteesEntity> defenseCommittees = new ArrayList<>();

    @OneToMany(mappedBy = "defensePeriod")
    @Builder.Default
    List<TeamEntity> teams = new ArrayList<>();

    public DefensePeriodConstain resolveStatus(LocalDate today) {
        if (today == null || startDate == null || endDate == null) {
            return status;
        }
        if (today.isBefore(startDate)) {
            return DefensePeriodConstain.PENDING;
        }
        if (today.isAfter(endDate)) {
            return DefensePeriodConstain.FINISHED;
        }
        return DefensePeriodConstain.ONGOING;
    }

    public void synchronizeStatus(LocalDate today) {
        DefensePeriodConstain resolvedStatus = resolveStatus(today);
        if (resolvedStatus != null) {
            status = resolvedStatus;
        }
    }

    public boolean isFinished() {
        return resolveStatus(LocalDate.now(BUSINESS_ZONE)) == DefensePeriodConstain.FINISHED;
    }

    @PostLoad
    @PrePersist
    @PreUpdate
    void synchronizeStatusWithCurrentDate() {
        synchronizeStatus(LocalDate.now(BUSINESS_ZONE));
    }
}

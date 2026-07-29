package com.graduration.entity;

import java.time.LocalDate;
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

    @Column(name = "project_type")
    String projectType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
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
}

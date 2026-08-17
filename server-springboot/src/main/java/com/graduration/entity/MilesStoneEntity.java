package com.graduration.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import com.graduration.Constain.MilesStoneStatusConstain;
import com.graduration.Constain.MilesStoneTypeConstain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
// Các mốc tiến độ / Giai đoạn
@Table(name = "Miles_Stone")
public class MilesStoneEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "Id_Miles_Stone")
    Long IdMilesStone;

    @Column(name = "Milestone_Name", nullable = false) // Tên mốc tiến độ
    String milesStoneName;

    @Column(name = "description") // Mô tả chi tiết yêu cầu của mốc đó
    String Description;

    @Column(name = "Deadline", nullable = false)
    LocalDateTime deadLine; // Hạn chót (ngày giờ) phải hoàn thành.

    @Column(name = "start_at")
    LocalDateTime startAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "milestone_type")
    MilesStoneTypeConstain milestoneType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    MilesStoneStatusConstain status;

    @Column(name = "allow_late_submission")
    @Builder.Default
    Boolean allowLateSubmission = true;

    @Column(name = "required")
    @Builder.Default
    Boolean required = true;

    @Column(name = "max_file_size")
    Long maxFileSize;

    @Column(name = "allowed_file_types")
    String allowedFileTypes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Id_Defense", nullable = false)
    DefensePeriodEntity defensePeriod;

    @OneToMany(mappedBy = "milesStone", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<SubmistionEntity> submistion = new ArrayList<>();

    @PrePersist
    void prePersist() {
        status = status == null ? MilesStoneStatusConstain.DRAFT : status;
        allowLateSubmission = allowLateSubmission == null || allowLateSubmission;
        required = required == null || required;
    }
}

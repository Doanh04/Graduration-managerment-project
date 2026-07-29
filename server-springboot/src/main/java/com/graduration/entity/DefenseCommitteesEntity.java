package com.graduration.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_id", nullable = false)
    AcademicYearEntity academicYear;

    @OneToMany(mappedBy = "defenseCommittees", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<ComitteesMemberEntity> comitteesMember = new ArrayList<>();

    @OneToMany(mappedBy = "defenseCommittees", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<DefenseSchedulesEntity> defenseSchedules = new ArrayList<>();
}

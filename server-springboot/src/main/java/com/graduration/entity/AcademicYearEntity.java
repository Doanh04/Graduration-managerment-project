package com.graduration.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "academic_year")
public class AcademicYearEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "academic_id")
    Integer academicId;

    @Column(name = "academic_year", nullable = false, unique = true)
    String academicYear;

    @Column(name = "description")
    String description;

    @OneToMany(mappedBy = "academicYear", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<DefensePeriodEntity> defensePeriod = new HashSet<>();
}

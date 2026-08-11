package com.graduration.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.graduration.entity.AcademicYearEntity;

public interface AcademicYearRepository extends JpaRepository<AcademicYearEntity, Integer> {
    Optional<AcademicYearEntity> findByAcademicYearIgnoreCase(String academicYear);

    boolean existsByAcademicYearIgnoreCase(String academicYear);

    boolean existsByAcademicYearIgnoreCaseAndAcademicIdNot(String academicYear, Integer academicId);
}

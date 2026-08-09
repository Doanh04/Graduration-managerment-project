package com.graduration.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.graduration.entity.ClassEntity;

public interface ClassRepository extends JpaRepository<ClassEntity, Long> {
    Optional<ClassEntity> findByClassCodeIgnoreCase(String classCode);

    boolean existsByClassCodeIgnoreCase(String classCode);

    boolean existsByClassNameIgnoreCase(String className);

    boolean existsByClassCodeIgnoreCaseAndClassIdNot(String classCode, Long classId);

    boolean existsByClassNameIgnoreCaseAndClassIdNot(String className, Long classId);
}

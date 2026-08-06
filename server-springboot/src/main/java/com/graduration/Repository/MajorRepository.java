package com.graduration.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.graduration.entity.MajorEntity;

@Repository
public interface MajorRepository extends JpaRepository<MajorEntity, Long> {
    boolean existsByMajorNameIgnoreCase(String majorName);

    boolean existsByMajorNameIgnoreCaseAndMajorIdNot(String majorName, Long majorId);
}

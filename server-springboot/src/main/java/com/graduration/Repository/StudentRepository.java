package com.graduration.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.graduration.entity.StudentEntity;

public interface StudentRepository extends JpaRepository<StudentEntity, String> {
    Optional<StudentEntity> findByStudentCodeIgnoreCase(String studentCode);

    Optional<StudentEntity> findByUserEntity_UserId(String userId);

    boolean existsByStudentCodeIgnoreCase(String studentCode);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhoneStudent(String phoneStudent);

    List<StudentEntity> findAllByStudentCodeIn(Iterable<String> studentCodes);
}

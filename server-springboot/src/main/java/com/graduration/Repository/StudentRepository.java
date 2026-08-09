package com.graduration.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.graduration.entity.StudentEntity;

public interface StudentRepository extends JpaRepository<StudentEntity, String> {}

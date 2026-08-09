package com.graduration.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.graduration.entity.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
    @EntityGraph(attributePaths = {"roles", "roles.permission", "student", "lecture"})
    Optional<UserEntity> findByUserName(String userName);

    @EntityGraph(attributePaths = {"roles", "roles.permission", "student", "lecture"})
    List<UserEntity> findByUserNameOrLecture_LectureCodeOrStudent_StudentCode(
            String userName, String lecturerCode, String studentCode);

    @Override
    @EntityGraph(attributePaths = {"roles", "roles.permission", "student", "lecture"})
    Optional<UserEntity> findById(String userId);

    boolean existsByUserName(String userName);

    boolean existsByUserNameAndUserIdNot(String userName, String userId);
}

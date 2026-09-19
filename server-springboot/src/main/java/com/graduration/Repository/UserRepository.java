package com.graduration.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.graduration.entity.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
    @Override
    @EntityGraph(attributePaths = {"roles", "roles.permission", "student", "lecture"})
    List<UserEntity> findAll();

    @EntityGraph(attributePaths = {"roles", "roles.permission", "student", "lecture"})
    Optional<UserEntity> findByUserName(String userName);

    @EntityGraph(attributePaths = {"roles", "roles.permission", "student", "lecture"})
    List<UserEntity> findByUserNameOrLecture_LectureCodeOrStudent_StudentCode(
            String userName, String lecturerCode, String studentCode);

    @Override
    @EntityGraph(attributePaths = {"roles", "roles.permission", "student", "lecture"})
    Optional<UserEntity> findById(String userId);

    /** Tìm tài khoản theo mọi định danh có thể được lưu ở các bản ghi nghiệp vụ cũ. */
    @EntityGraph(attributePaths = {"roles", "roles.permission", "student", "lecture"})
    @Query(
            """
			select distinct u from UserEntity u
			left join u.lecture lecturer
			left join u.student student
			where u.userId = :identifier
			or u.userName = :identifier
			or lecturer.lectureId = :identifier
			or lecturer.lectureCode = :identifier
			or student.idStudent = :identifier
			or student.studentCode = :identifier
			""")
    Optional<UserEntity> findByAnyIdentifier(@Param("identifier") String identifier);

    boolean existsByUserName(String userName);

    boolean existsByUserNameAndUserIdNot(String userName, String userId);
}

package com.graduration.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.graduration.entity.LectureEntity;

@Repository
public interface LectureRepository extends JpaRepository<LectureEntity, String> {
    @Query(
            """
			select lecturer from LectureEntity lecturer
			where lower(lecturer.fullNameLecture) like lower(concat('%', :keyword, '%'))
			or lower(lecturer.lectureCode) like lower(concat('%', :keyword, '%'))
			""")
    Page<LectureEntity> searchByNameOrCode(@Param("keyword") String keyword, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"user", "user.roles", "user.roles.permission"})
    List<LectureEntity> findAll();

    boolean existsByLectureCode(String lectureCode);

    boolean existsByEmaillecture(String email);

    boolean existsByPhoneLecture(String phone);

    Optional<LectureEntity> findByUser_UserId(String userId);

    @EntityGraph(attributePaths = {"user", "user.roles", "user.roles.permission"})
    Optional<LectureEntity> findByUser_UserName(String userName);

    boolean existsByLectureCodeAndLectureIdNot(String lectureCode, String lectureId);

    boolean existsByEmaillectureAndLectureIdNot(String email, String lectureId);

    boolean existsByPhoneLectureAndLectureIdNot(String phone, String lectureId);
}

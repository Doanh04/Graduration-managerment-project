package com.graduration.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.graduration.entity.StudentEntity;

public interface StudentRepository extends JpaRepository<StudentEntity, String> {
    @Query(
            """
			select student from StudentEntity student
			where lower(student.fullNameStudent) like lower(concat('%', :keyword, '%'))
			or lower(student.studentCode) like lower(concat('%', :keyword, '%'))
			""")
    Page<StudentEntity> searchByNameOrCode(@Param("keyword") String keyword, Pageable pageable);

    Optional<StudentEntity> findByStudentCodeIgnoreCase(String studentCode);

    Optional<StudentEntity> findByUserEntity_UserId(String userId);

    boolean existsByStudentCodeIgnoreCase(String studentCode);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhoneStudent(String phoneStudent);

    boolean existsByStudentCodeIgnoreCaseAndIdStudentNot(String studentCode, String idStudent);

    boolean existsByEmailIgnoreCaseAndIdStudentNot(String email, String idStudent);

    boolean existsByPhoneStudentAndIdStudentNot(String phoneStudent, String idStudent);

    List<StudentEntity> findAllByStudentCodeIn(Iterable<String> studentCodes);

    @Query(
            """
			select student from StudentEntity student
			left join fetch student.classEntity
			left join fetch student.userEntity user
			where user.createAt >= :start and user.createAt < :end
			order by student.fullNameStudent, student.studentCode
			""")
    List<StudentEntity> findForExportByCreatedAt(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(
            """
			select distinct year(student.userEntity.createAt) from StudentEntity student
			where student.userEntity.createAt is not null
			order by year(student.userEntity.createAt) desc
			""")
    List<Integer> findDistinctCreationYears();
}

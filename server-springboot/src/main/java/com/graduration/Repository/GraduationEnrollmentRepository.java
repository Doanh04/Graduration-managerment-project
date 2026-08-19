package com.graduration.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.graduration.entity.GraduationEnrollmentEntity;
import com.graduration.entity.StudentEntity;

public interface GraduationEnrollmentRepository extends JpaRepository<GraduationEnrollmentEntity, Long> {
    @Query(
            """
			select case when count(enrollment) > 0 then true else false end
			from GraduationEnrollmentEntity enrollment
			where enrollment.student.idStudent = :studentId
			and enrollment.defensePeriod.ID_Defense = :defensePeriodId
			""")
    boolean existsByStudent_IdStudentAndDefensePeriod_ID_Defense(
            @Param("studentId") String studentId, @Param("defensePeriodId") Long defensePeriodId);

    @Query(
            """
			select distinct student from GraduationEnrollmentEntity enrollment
			join enrollment.student student
			left join fetch student.classEntity
			left join fetch student.userEntity
			where enrollment.defensePeriod.academicYear.academicId = :academicYearId
			order by student.fullNameStudent, student.studentCode
			""")
    List<StudentEntity> findDistinctStudentsByAcademicYearId(@Param("academicYearId") Integer academicYearId);
}

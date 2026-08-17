package com.graduration.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.graduration.entity.GraduationEnrollmentEntity;

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
}

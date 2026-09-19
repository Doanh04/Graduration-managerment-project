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

    @Query(
            """
			select distinct student from StudentEntity student
			left join student.graduationEnrollments enrollment
			left join enrollment.defensePeriod period
			where (:keyword is null or :keyword = ''
				or lower(student.fullNameStudent) like lower(concat('%', :keyword, '%'))
				or lower(student.studentCode) like lower(concat('%', :keyword, '%')))
			and (:academicYearId is null or period.academicYear.academicId = :academicYearId)
			and (:defensePeriodId is null or period.ID_Defense = :defensePeriodId)
			and (:classCode is null or lower(student.classEntity.classCode) = lower(:classCode))
			""")
    Page<StudentEntity> searchByKeywordAndAcademicYearAndDefensePeriod(
            @Param("keyword") String keyword,
            @Param("academicYearId") Integer academicYearId,
            @Param("defensePeriodId") Long defensePeriodId,
            @Param("classCode") String classCode,
            Pageable pageable);

    Optional<StudentEntity> findByStudentCodeIgnoreCase(String studentCode);

    Optional<StudentEntity> findByUserEntity_UserId(String userId);

    /** Tìm hồ sơ sinh viên theo username tài khoản, dùng để hiển thị người đề xuất ở các bản ghi cũ. */
    Optional<StudentEntity> findByUserEntity_UserName(String userName);

    /** Tra cứu tên sinh viên theo user, mã hồ sơ hoặc mã sinh viên được lưu trong đề xuất. */
    @Query(
            """
			select student.fullNameStudent
			from StudentEntity student
			left join student.userEntity user
			where student.idStudent = :identifier
			or lower(student.studentCode) = lower(:identifier)
			or user.userId = :identifier
			or lower(user.userName) = lower(:identifier)
			""")
    Optional<String> findDisplayNameByAnyIdentifier(@Param("identifier") String identifier);

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
			select student from StudentEntity student
			where exists (
				select enrollment.enrollmentId
				from GraduationEnrollmentEntity enrollment
				where enrollment.student.idStudent = student.idStudent
				and enrollment.defensePeriod.academicYear.academicId = :academicYearId
				and enrollment.defensePeriod.ID_Defense = :defensePeriodId
			)
			order by student.fullNameStudent, student.studentCode
			""")
    List<StudentEntity> findForExportByAcademicYearAndDefensePeriod(
            @Param("academicYearId") Integer academicYearId, @Param("defensePeriodId") Long defensePeriodId);

    @Query(
            """
			select distinct year(student.userEntity.createAt) from StudentEntity student
			where student.userEntity.createAt is not null
			order by year(student.userEntity.createAt) desc
			""")
    List<Integer> findDistinctCreationYears();
}

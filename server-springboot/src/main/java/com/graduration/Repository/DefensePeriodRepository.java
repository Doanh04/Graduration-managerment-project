package com.graduration.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.graduration.Constain.DefensePeriodConstain;
import com.graduration.entity.DefensePeriodEntity;

public interface DefensePeriodRepository extends JpaRepository<DefensePeriodEntity, Long> {
    List<DefensePeriodEntity> findAllByOrderByStartDateDesc();

    Page<DefensePeriodEntity> findAllByOrderByStartDateDesc(Pageable pageable);

    List<DefensePeriodEntity> findAllByAcademicYear_AcademicIdOrderByStartDateDesc(Integer academicId);

    Page<DefensePeriodEntity> findAllByAcademicYear_AcademicIdOrderByStartDateDesc(
            Integer academicId, Pageable pageable);

    boolean existsByPeriodNameIgnoreCaseAndAcademicYear_AcademicId(String periodName, Integer academicId);

    @Query(
            """
			select case when count(period) > 0 then true else false end
			from DefensePeriodEntity period
			where lower(period.periodName) = lower(:periodName)
			and period.academicYear.academicId = :academicId
			and period.ID_Defense <> :defensePeriodId
			""")
    boolean existsDuplicatePeriodName(
            @Param("periodName") String periodName,
            @Param("academicId") Integer academicId,
            @Param("defensePeriodId") Long defensePeriodId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
			update DefensePeriodEntity period
			set period.status = :finished
			where period.endDate < :today and period.status <> :finished
			""")
    int markExpiredPeriodsFinished(@Param("today") LocalDate today, @Param("finished") DefensePeriodConstain finished);
}

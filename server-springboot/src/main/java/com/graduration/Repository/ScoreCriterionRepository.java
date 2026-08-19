package com.graduration.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.graduration.entity.ScoreCriterionEntity;

public interface ScoreCriterionRepository extends JpaRepository<ScoreCriterionEntity, Long> {
    @Query(
            """
			select criterion from ScoreCriterionEntity criterion
			where criterion.defensePeriod.ID_Defense = :defensePeriodId
			order by criterion.displayOrder asc, criterion.criterionId asc
			""")
    Page<ScoreCriterionEntity> findByDefensePeriod(@Param("defensePeriodId") Long defensePeriodId, Pageable pageable);

    @Query(
            """
			select criterion from ScoreCriterionEntity criterion
			where criterion.defensePeriod.ID_Defense = :defensePeriodId
			and criterion.active = true
			order by criterion.displayOrder asc, criterion.criterionId asc
			""")
    List<ScoreCriterionEntity> findActiveByDefensePeriod(@Param("defensePeriodId") Long defensePeriodId);

    @Query(
            """
			select case when count(criterion) > 0 then true else false end
			from ScoreCriterionEntity criterion
			where lower(criterion.criterionCode) = lower(:code)
			and criterion.defensePeriod.ID_Defense = :defensePeriodId
			and (:criterionId is null or criterion.criterionId <> :criterionId)
			""")
    boolean existsDuplicateCode(
            @Param("code") String code,
            @Param("defensePeriodId") Long defensePeriodId,
            @Param("criterionId") Long criterionId);
}

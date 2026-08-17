package com.graduration.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.graduration.entity.MilesStoneEntity;

public interface MilestoneRepository
        extends JpaRepository<MilesStoneEntity, Long>, JpaSpecificationExecutor<MilesStoneEntity> {
    @Query(
            """
			select case when count(milestone) > 0 then true else false end
			from MilesStoneEntity milestone
			where lower(milestone.milesStoneName) = lower(:name)
			and milestone.defensePeriod.ID_Defense = :defensePeriodId
			""")
    boolean existsNameInDefensePeriod(@Param("name") String name, @Param("defensePeriodId") Long defensePeriodId);

    @Query(
            """
			select case when count(milestone) > 0 then true else false end
			from MilesStoneEntity milestone
			where lower(milestone.milesStoneName) = lower(:name)
			and milestone.defensePeriod.ID_Defense = :defensePeriodId
			and milestone.IdMilesStone <> :milestoneId
			""")
    boolean existsDuplicateName(
            @Param("name") String name,
            @Param("defensePeriodId") Long defensePeriodId,
            @Param("milestoneId") Long milestoneId);
}

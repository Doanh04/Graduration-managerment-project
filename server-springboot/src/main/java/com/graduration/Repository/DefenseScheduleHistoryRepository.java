package com.graduration.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.graduration.entity.DefenseScheduleHistoryEntity;

@Repository
public interface DefenseScheduleHistoryRepository extends JpaRepository<DefenseScheduleHistoryEntity, Long> {
    @EntityGraph(attributePaths = {"changedBy"})
    @Query(
            """
			select history from DefenseScheduleHistoryEntity history
			where history.schedule.idDefenseScheduce = :scheduleId
			order by history.changedAt desc, history.historyId desc
			""")
    Page<DefenseScheduleHistoryEntity> findByScheduleId(@Param("scheduleId") Long scheduleId, Pageable pageable);

    boolean existsBySchedule_IdDefenseScheduce(Long scheduleId);
}

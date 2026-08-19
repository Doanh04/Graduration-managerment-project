package com.graduration.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.graduration.Constain.CommitteeMemberStatusConstain;
import com.graduration.Constain.DefenseScheduleStatusConstain;
import com.graduration.entity.DefenseSchedulesEntity;

@Repository
public interface DefenseScheduleRepository extends JpaRepository<DefenseSchedulesEntity, Long> {
    @Override
    @EntityGraph(attributePaths = {"topic", "topic.defensePeriod", "topic.team", "defenseCommittees", "createdBy"})
    Optional<DefenseSchedulesEntity> findById(Long scheduleId);

    @Query(
            value =
                    """
					select schedule from DefenseSchedulesEntity schedule
					where schedule.topic.defensePeriod.ID_Defense = :periodId
					and (:date is null or schedule.defenseDate = :date)
					and (:committeeId is null or schedule.defenseCommittees.idComittees = :committeeId)
					and (:room is null or lower(schedule.room) = lower(:room))
					and (:status is null or schedule.status = :status)
					order by schedule.defenseDate, schedule.startTime
					""",
            countQuery =
                    """
					select count(schedule) from DefenseSchedulesEntity schedule
					where schedule.topic.defensePeriod.ID_Defense = :periodId
					and (:date is null or schedule.defenseDate = :date)
					and (:committeeId is null or schedule.defenseCommittees.idComittees = :committeeId)
					and (:room is null or lower(schedule.room) = lower(:room))
					and (:status is null or schedule.status = :status)
					""")
    Page<DefenseSchedulesEntity> findByPeriod(
            @Param("periodId") Long periodId,
            @Param("date") LocalDate date,
            @Param("committeeId") Long committeeId,
            @Param("room") String room,
            @Param("status") DefenseScheduleStatusConstain status,
            Pageable pageable);

    @Query(
            """
			select count(schedule) > 0 from DefenseSchedulesEntity schedule
			where schedule.defenseDate = :date
			and lower(schedule.room) = lower(:room)
			and schedule.startTime < :endTime
			and schedule.endTime > :startTime
			and schedule.status <> :cancelled
			and (:excludedId is null or schedule.idDefenseScheduce <> :excludedId)
			""")
    boolean hasRoomConflict(
            @Param("date") LocalDate date,
            @Param("room") String room,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("cancelled") DefenseScheduleStatusConstain cancelled,
            @Param("excludedId") Long excludedId);

    @Query(
            """
			select count(schedule) > 0 from DefenseSchedulesEntity schedule
			where schedule.defenseDate = :date
			and schedule.defenseCommittees.idComittees = :committeeId
			and schedule.startTime < :endTime
			and schedule.endTime > :startTime
			and schedule.status <> :cancelled
			and (:excludedId is null or schedule.idDefenseScheduce <> :excludedId)
			""")
    boolean hasCommitteeConflict(
            @Param("date") LocalDate date,
            @Param("committeeId") Long committeeId,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("cancelled") DefenseScheduleStatusConstain cancelled,
            @Param("excludedId") Long excludedId);

    @Query(
            """
			select count(schedule) > 0 from DefenseSchedulesEntity schedule
			join schedule.defenseCommittees.comitteesMember member
			where schedule.defenseDate = :date
			and member.lecture.lectureId in (
				select requestedMember.lecture.lectureId
				from ComitteesMemberEntity requestedMember
				where requestedMember.defenseCommittees.idComittees = :committeeId
					and requestedMember.status = :memberStatus
			)
			and member.status = :memberStatus
			and schedule.startTime < :endTime
			and schedule.endTime > :startTime
			and schedule.status <> :cancelled
			and (:excludedId is null or schedule.idDefenseScheduce <> :excludedId)
			""")
    boolean hasLecturerConflict(
            @Param("date") LocalDate date,
            @Param("committeeId") Long committeeId,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("memberStatus") CommitteeMemberStatusConstain memberStatus,
            @Param("cancelled") DefenseScheduleStatusConstain cancelled,
            @Param("excludedId") Long excludedId);
}

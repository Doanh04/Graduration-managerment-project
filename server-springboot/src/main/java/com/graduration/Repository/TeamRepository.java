package com.graduration.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.graduration.entity.TeamEntity;

public interface TeamRepository extends JpaRepository<TeamEntity, Long> {
    @EntityGraph(
            attributePaths = {
                "topic",
                "defensePeriod",
                "defensePeriod.academicYear",
                "studentEntities",
                "studentEntities.classEntity",
                "studentEntities.userEntity"
            })
    Optional<TeamEntity> findWithDetailsByIdTeam(Long idTeam);

    @EntityGraph(
            attributePaths = {
                "topic",
                "defensePeriod",
                "defensePeriod.academicYear",
                "studentEntities",
                "studentEntities.classEntity",
                "studentEntities.userEntity"
            })
    Optional<TeamEntity> findByStudentEntities_UserEntity_UserId(String userId);

    @EntityGraph(
            attributePaths = {
                "topic",
                "defensePeriod",
                "defensePeriod.academicYear",
                "studentEntities",
                "studentEntities.classEntity"
            })
    List<TeamEntity> findAllByOrderByIdTeamAsc();

    @EntityGraph(
            attributePaths = {
                "topic",
                "defensePeriod",
                "defensePeriod.academicYear",
                "studentEntities",
                "studentEntities.classEntity"
            })
    Page<TeamEntity> findAllByOrderByIdTeamAsc(Pageable pageable);

    @EntityGraph(
            attributePaths = {
                "topic",
                "defensePeriod",
                "defensePeriod.academicYear",
                "studentEntities",
                "studentEntities.classEntity",
                "studentEntities.userEntity"
            })
    @Query(
            "select team from TeamEntity team where team.defensePeriod.ID_Defense = :defensePeriodId order by team.idTeam asc")
    Page<TeamEntity> findAllByDefensePeriodId(@Param("defensePeriodId") Long defensePeriodId, Pageable pageable);

    @EntityGraph(
            attributePaths = {
                "topic",
                "defensePeriod",
                "defensePeriod.academicYear",
                "studentEntities",
                "studentEntities.classEntity",
                "studentEntities.userEntity"
            })
    Optional<TeamEntity> findFirstByStudentEntities_UserEntity_UserIdOrderByDefensePeriod_EndDateDesc(String userId);

    @EntityGraph(
            attributePaths = {
                "topic",
                "defensePeriod",
                "defensePeriod.academicYear",
                "studentEntities",
                "studentEntities.classEntity",
                "studentEntities.userEntity"
            })
    List<TeamEntity> findAllByStudentEntities_UserEntity_UserIdOrderByDefensePeriod_EndDateDesc(String userId);

    boolean existsByNameTeamIgnoreCase(String nameTeam);

    boolean existsByNameTeamIgnoreCaseAndIdTeamNot(String nameTeam, Long idTeam);

    @Query(
            """
			select case when count(team) > 0 then true else false end
			from TeamEntity team
			where lower(team.nameTeam) = lower(:nameTeam)
			and team.defensePeriod.ID_Defense = :defensePeriodId
			""")
    boolean existsByNameAndDefensePeriod(
            @Param("nameTeam") String nameTeam, @Param("defensePeriodId") Long defensePeriodId);

    @Query(
            """
			select case when count(team) > 0 then true else false end
			from TeamEntity team
			where lower(team.nameTeam) = lower(:nameTeam)
			and team.defensePeriod.ID_Defense = :defensePeriodId
			and team.idTeam <> :teamId
			""")
    boolean existsByNameAndDefensePeriodAndIdNot(
            @Param("nameTeam") String nameTeam,
            @Param("defensePeriodId") Long defensePeriodId,
            @Param("teamId") Long teamId);

    @Query(
            """
			select case when count(team) > 0 then true else false end
			from TeamEntity team join team.studentEntities student
			where student.idStudent = :studentId
			and team.defensePeriod.ID_Defense = :defensePeriodId
			and team.idTeam <> :teamId
			""")
    boolean existsStudentInAnotherTeamOfDefensePeriod(
            @Param("studentId") String studentId,
            @Param("defensePeriodId") Long defensePeriodId,
            @Param("teamId") Long teamId);

    boolean existsByTopic_IdTopic(Long topicId);

    boolean existsByTopic_IdTopicAndIdTeamNot(Long topicId, Long idTeam);
}

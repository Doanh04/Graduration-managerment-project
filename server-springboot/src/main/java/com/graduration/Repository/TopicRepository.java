package com.graduration.Repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.graduration.Constain.SupervisorAssignmentStatusConstain;
import com.graduration.Constain.SupervisorRoleConstain;
import com.graduration.Constain.TopicStatusConstain;
import com.graduration.entity.TopicEntity;

public interface TopicRepository extends JpaRepository<TopicEntity, Long>, JpaSpecificationExecutor<TopicEntity> {
    List<TopicEntity> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    boolean existsByCreatedByAndStatusIn(String createdBy, Collection<TopicStatusConstain> statuses);

    /**
     * Loads only topics that can be placed on a defense schedule. A real team,
     * an active primary supervisor and an unscheduled topic are required; this
     * keeps the schedule form consistent with the same rules enforced on save.
     */
    @Query(
            """
			select distinct topic from TopicEntity topic
			join fetch topic.team
			join fetch topic.defensePeriod period
			left join fetch period.academicYear
			join fetch topic.topicSuperVisorEntities supervisor
			where period.ID_Defense = :periodId
			and topic.status in :statuses
			and supervisor.status = :supervisorStatus
			and supervisor.supervisorRole = :supervisorRole
			and not exists (
				select schedule.idDefenseScheduce
				from DefenseSchedulesEntity schedule
				where schedule.topic.idTopic = topic.idTopic
			)
			order by topic.createdAt desc
			""")
    List<TopicEntity> findEligibleForDefenseSchedule(
            @Param("periodId") Long periodId,
            @Param("statuses") Collection<TopicStatusConstain> statuses,
            @Param("supervisorStatus") SupervisorAssignmentStatusConstain supervisorStatus,
            @Param("supervisorRole") SupervisorRoleConstain supervisorRole);

    @Query(
            """
			select distinct topic from TopicEntity topic
			where topic.categoryTopic = com.graduration.Constain.CategoryTopicConstain.STUDENT
			and topic.createdBy in (
				select student.userEntity.userId from TeamEntity team join team.studentEntities student where team.idTeam = :teamId
			)
			order by topic.createdAt desc
			""")
    List<TopicEntity> findStudentProposalsByTeam(@Param("teamId") Long teamId);

    @Query(
            """
			select case when count(topic) > 0 then true else false end
			from TopicEntity topic
			where lower(topic.title) = lower(:title)
			and topic.defensePeriod.ID_Defense = :defensePeriodId
			""")
    boolean existsTitleInDefensePeriod(@Param("title") String title, @Param("defensePeriodId") Long defensePeriodId);

    @Query(
            """
			select case when count(topic) > 0 then true else false end
			from TopicEntity topic
			where lower(topic.title) = lower(:title)
			and topic.defensePeriod.ID_Defense = :defensePeriodId
			and topic.idTopic <> :topicId
			""")
    boolean existsDuplicateTitle(
            @Param("title") String title,
            @Param("defensePeriodId") Long defensePeriodId,
            @Param("topicId") Long topicId);
}

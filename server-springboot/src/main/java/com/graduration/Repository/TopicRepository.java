package com.graduration.Repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.graduration.Constain.TopicStatusConstain;
import com.graduration.entity.TopicEntity;

public interface TopicRepository extends JpaRepository<TopicEntity, Long>, JpaSpecificationExecutor<TopicEntity> {
    List<TopicEntity> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    boolean existsByCreatedByAndStatusIn(String createdBy, Collection<TopicStatusConstain> statuses);

    boolean existsByProposedTeam_IdTeamAndStatusIn(Long teamId, Collection<TopicStatusConstain> statuses);

    @Query(
            """
			select distinct topic from TopicEntity topic
			where topic.categoryTopic = com.graduration.Constain.CategoryTopicConstain.STUDENT
			and (topic.proposedTeam.idTeam = :teamId or topic.createdBy in (
				select student.userEntity.userId from StudentEntity student where student.team.idTeam = :teamId
			))
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

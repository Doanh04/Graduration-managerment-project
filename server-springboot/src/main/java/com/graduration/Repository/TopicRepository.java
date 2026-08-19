package com.graduration.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.graduration.entity.TopicEntity;

public interface TopicRepository extends JpaRepository<TopicEntity, Long>, JpaSpecificationExecutor<TopicEntity> {
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

package com.graduration.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.graduration.entity.TeamEntity;

public interface TeamRepository extends JpaRepository<TeamEntity, Long> {
    @EntityGraph(attributePaths = {"topic", "studentEntities", "studentEntities.classEntity"})
    Optional<TeamEntity> findWithDetailsByIdTeam(Long idTeam);

    @EntityGraph(attributePaths = {"topic", "studentEntities", "studentEntities.classEntity"})
    List<TeamEntity> findAllByOrderByIdTeamAsc();

    @EntityGraph(attributePaths = {"topic", "studentEntities", "studentEntities.classEntity"})
    Page<TeamEntity> findAllByOrderByIdTeamAsc(Pageable pageable);

    boolean existsByNameTeamIgnoreCase(String nameTeam);

    boolean existsByNameTeamIgnoreCaseAndIdTeamNot(String nameTeam, Long idTeam);

    boolean existsByTopic_IdTopic(Long topicId);

    boolean existsByTopic_IdTopicAndIdTeamNot(Long topicId, Long idTeam);
}

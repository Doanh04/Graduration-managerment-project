package com.graduration.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.graduration.entity.SubmistionEntity;

public interface SubmissionRepository
        extends JpaRepository<SubmistionEntity, Long>, JpaSpecificationExecutor<SubmistionEntity> {
    @Query(
            """
			select submission from SubmistionEntity submission
			where submission.team.idTeam = :teamId
			and submission.milesStone.IdMilesStone = :milestoneId
			and submission.version = (
				select max(candidate.version) from SubmistionEntity candidate
				where candidate.team.idTeam = :teamId
				and candidate.milesStone.IdMilesStone = :milestoneId
			)
			""")
    Optional<SubmistionEntity> findFirstByTeam_IdTeamAndMilesStone_IdMilesStoneOrderByVersionDesc(
            @Param("teamId") Long teamId, @Param("milestoneId") Long milestoneId);

    @Query(
            """
			select submission from SubmistionEntity submission
			where submission.team.idTeam = :teamId
			and submission.milesStone.IdMilesStone = :milestoneId
			order by submission.version desc
			""")
    Page<SubmistionEntity> findByTeam_IdTeamAndMilesStone_IdMilesStoneOrderByVersionDesc(
            @Param("teamId") Long teamId, @Param("milestoneId") Long milestoneId, Pageable pageable);
}

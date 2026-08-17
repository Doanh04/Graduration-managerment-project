package com.graduration.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.graduration.Constain.DefenseCommitteeStatusConstain;
import com.graduration.entity.DefenseCommitteesEntity;

@Repository
public interface DefenseCommitteeRepository extends JpaRepository<DefenseCommitteesEntity, Long> {
    @Override
    @EntityGraph(attributePaths = {"defensePeriod", "academicYear"})
    Optional<DefenseCommitteesEntity> findById(Long committeeId);

    @Query(
            value =
                    """
					select committee from DefenseCommitteesEntity committee
					where committee.defensePeriod.ID_Defense = :defensePeriodId
					and (:status is null or committee.status = :status)
					and (:keyword is null or lower(committee.comitteesName) like lower(concat('%', :keyword, '%')))
					order by committee.createdAt desc, committee.idComittees desc
					""",
            countQuery =
                    """
					select count(committee) from DefenseCommitteesEntity committee
					where committee.defensePeriod.ID_Defense = :defensePeriodId
					and (:status is null or committee.status = :status)
					and (:keyword is null or lower(committee.comitteesName) like lower(concat('%', :keyword, '%')))
					""")
    Page<DefenseCommitteesEntity> findByDefensePeriod(
            @Param("defensePeriodId") Long defensePeriodId,
            @Param("status") DefenseCommitteeStatusConstain status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query(
            """
			select count(committee) > 0 from DefenseCommitteesEntity committee
			where lower(committee.comitteesName) = lower(:name)
			and committee.defensePeriod.ID_Defense = :defensePeriodId
			""")
    boolean existsNameInDefensePeriod(@Param("name") String name, @Param("defensePeriodId") Long defensePeriodId);

    @Query(
            """
			select count(committee) > 0 from DefenseCommitteesEntity committee
			where lower(committee.comitteesName) = lower(:name)
			and committee.defensePeriod.ID_Defense = :defensePeriodId
			and committee.idComittees <> :committeeId
			""")
    boolean existsDuplicateName(
            @Param("name") String name,
            @Param("defensePeriodId") Long defensePeriodId,
            @Param("committeeId") Long committeeId);
}

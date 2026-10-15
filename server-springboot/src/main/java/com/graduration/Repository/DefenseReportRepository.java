package com.graduration.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.graduration.entity.DefenseReportEntity;

public interface DefenseReportRepository extends JpaRepository<DefenseReportEntity, Long> {
    List<DefenseReportEntity> findBySchedule_IdDefenseScheduceOrderByRevisionDesc(Long scheduleId);
    List<DefenseReportEntity> findByReviewAssignment_ReviewAssignmentIdOrderByRevisionDesc(Long assignmentId);
}

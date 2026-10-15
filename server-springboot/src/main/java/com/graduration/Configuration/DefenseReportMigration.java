package com.graduration.Configuration;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.graduration.Repository.DefenseScheduleRepository;
import com.graduration.Constain.DefenseScheduleStatusConstain;
import lombok.RequiredArgsConstructor;

@Component @Order(200) @RequiredArgsConstructor
public class DefenseReportMigration implements ApplicationRunner {
    private final DefenseScheduleRepository schedules;
    private final com.graduration.Repository.ScoreRepository scores;
    private final JdbcTemplate jdbc;

    @Override @Transactional
    public void run(ApplicationArguments args) {
        migrateReviewAssignmentStatus();
        migrateScoreCommentLength();

        // Preserve the actual FK column type while allowing system-generated results.
        var columns = jdbc.queryForList("select COLUMN_TYPE, IS_NULLABLE from information_schema.columns where table_schema = database() and table_name = 'score' and column_name = 'lecture_id'");
        for (var column : columns) {
            if ("NO".equals(column.get("IS_NULLABLE"))) {
                String type = String.valueOf(column.get("COLUMN_TYPE"));
                if (!type.matches("[a-zA-Z0-9(), ]+")) throw new IllegalStateException("Unexpected lecturer column type");
                jdbc.execute("alter table score modify lecture_id " + type + " NULL");
            }
        }
        var scheduleColumns = jdbc.queryForList("select COLUMN_TYPE, IS_NULLABLE from information_schema.columns where table_schema = database() and table_name = 'defense_report' and column_name = 'schedule_id'");
        for (var column : scheduleColumns) {
            if ("NO".equals(column.get("IS_NULLABLE"))) {
                String type = String.valueOf(column.get("COLUMN_TYPE"));
                if (!type.matches("[a-zA-Z0-9(), ]+")) throw new IllegalStateException("Unexpected schedule column type");
                jdbc.execute("alter table defense_report modify schedule_id " + type + " NULL");
            }
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        for (var schedule : schedules.findByReportRequiredIsNull()) {
            boolean upcoming = schedule.getDefenseDate() != null && schedule.getStartTime() != null
                    && LocalDateTime.of(schedule.getDefenseDate(), schedule.getStartTime()).isAfter(now);
            schedule.setReportRequired(upcoming && schedule.getStatus() != DefenseScheduleStatusConstain.COMPLETED
                    && schedule.getStatus() != DefenseScheduleStatusConstain.CANCELLED
                    && !scores.existsByTopic_IdTopic(schedule.getTopic().getIdTopic()));
        }
    }

    private void migrateReviewAssignmentStatus() {
        var columns = jdbc.queryForList(
                "select DATA_TYPE from information_schema.columns "
                        + "where table_schema = database() "
                        + "and table_name = 'review_assignment' and column_name = 'status'");
        if (!columns.isEmpty()
                && !"varchar".equalsIgnoreCase(String.valueOf(columns.get(0).get("DATA_TYPE")))) {
            // MySQL ENUM does not expand automatically when a Java enum gains a new state.
            // VARCHAR preserves existing values and allows NOT_ELIGIBLE plus future workflow states.
            jdbc.execute("alter table review_assignment modify column status varchar(32) not null");
        }
    }

    private void migrateScoreCommentLength() {
        var columns = jdbc.queryForList(
                "select CHARACTER_MAXIMUM_LENGTH from information_schema.columns "
                        + "where table_schema = database() "
                        + "and table_name = 'score' and column_name = 'comment'");
        if (!columns.isEmpty()) {
            Object value = columns.get(0).get("CHARACTER_MAXIMUM_LENGTH");
            long currentLength = value instanceof Number number ? number.longValue() : 0L;
            if (currentLength < 4000L) {
                jdbc.execute("alter table score modify column comment varchar(4000) null");
            }
        }
    }
}

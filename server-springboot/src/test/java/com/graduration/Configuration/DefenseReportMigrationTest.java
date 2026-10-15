package com.graduration.Configuration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.graduration.Repository.*;
import com.graduration.Constain.DefenseScheduleStatusConstain;
import com.graduration.entity.*;

class DefenseReportMigrationTest {
    @Test void migratesReviewAssignmentStatusEnumToVarchar() {
        var schedules = mock(DefenseScheduleRepository.class);
        var scores = mock(ScoreRepository.class);
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(contains("table_name = 'review_assignment'")))
                .thenReturn(List.of(Map.of("DATA_TYPE", "enum")));

        new DefenseReportMigration(schedules, scores, jdbc).run(null);

        verify(jdbc).execute("alter table review_assignment modify column status varchar(32) not null");
    }

    @Test void keepsReviewAssignmentStatusWhenAlreadyVarchar() {
        var schedules = mock(DefenseScheduleRepository.class);
        var scores = mock(ScoreRepository.class);
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(contains("table_name = 'review_assignment'")))
                .thenReturn(List.of(Map.of("DATA_TYPE", "varchar")));

        new DefenseReportMigration(schedules, scores, jdbc).run(null);

        verify(jdbc, never()).execute(contains("alter table review_assignment modify column status"));
    }

    @Test void expandsScoreCommentForMemberFeedback() {
        var schedules = mock(DefenseScheduleRepository.class);
        var scores = mock(ScoreRepository.class);
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(contains("table_name = 'score' and column_name = 'comment'")))
                .thenReturn(List.of(Map.of("CHARACTER_MAXIMUM_LENGTH", 255L)));

        new DefenseReportMigration(schedules, scores, jdbc).run(null);

        verify(jdbc).execute("alter table score modify column comment varchar(4000) null");
    }

    @Test void onlyUpcomingUnscoredSchedulesRequireReports() {
        var schedules = mock(DefenseScheduleRepository.class);
        var scores = mock(ScoreRepository.class);
        var jdbc = mock(JdbcTemplate.class);
        var future = schedule(1L, 1, DefenseScheduleStatusConstain.PUBLISHED);
        var past = schedule(2L, -1, DefenseScheduleStatusConstain.PUBLISHED);
        var completed = schedule(3L, 1, DefenseScheduleStatusConstain.COMPLETED);
        var cancelled = schedule(4L, 1, DefenseScheduleStatusConstain.CANCELLED);
        var scored = schedule(5L, 1, DefenseScheduleStatusConstain.PUBLISHED);
        when(scores.existsByTopic_IdTopic(5L)).thenReturn(true);
        when(schedules.findByReportRequiredIsNull()).thenReturn(List.of(future, past, completed, cancelled, scored));
        new DefenseReportMigration(schedules, scores, jdbc).run(null);
        assertTrue(future.getReportRequired());
        for (var s : List.of(past, completed, cancelled, scored)) assertFalse(s.getReportRequired());
        verifyNoInteractionsExceptRead(scores);
    }
    private void verifyNoInteractionsExceptRead(ScoreRepository scores) {
        verify(scores, never()).save(any());
    }
    private DefenseSchedulesEntity schedule(Long topicId, int days, DefenseScheduleStatusConstain status) {
        return DefenseSchedulesEntity.builder().status(status)
                .defenseDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(days)).startTime(LocalTime.NOON)
                .topic(TopicEntity.builder().idTopic(topicId).build()).build();
    }
}

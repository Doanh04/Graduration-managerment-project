package com.graduration.Configuration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** Preserves existing team members when the period-scoped membership table is introduced. */
@Component
@Order(100)
@RequiredArgsConstructor
public class TeamMembershipSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Integer tableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = database() and table_name = 'team_student'",
                Integer.class);
        if (tableCount != null && tableCount > 0) {
            Integer legacyColumnCount = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.columns "
                            + "where table_schema = database() and table_name = 'student' and column_name = 'id_team'",
                    Integer.class);
            if (legacyColumnCount != null && legacyColumnCount > 0) {
                jdbcTemplate.execute("insert ignore into team_student (team_id, student_id) "
                        + "select id_team, id_student from student where id_team is not null");
                jdbcTemplate
                        .queryForList(
                                "select constraint_name from information_schema.key_column_usage "
                                        + "where table_schema = database() and table_name = 'student' "
                                        + "and column_name = 'id_team' and referenced_table_name is not null",
                                String.class)
                        .forEach(constraint ->
                                jdbcTemplate.execute("alter table student drop foreign key `" + constraint + "`"));
                jdbcTemplate.execute("alter table student drop column id_team");
            }
        }
    }
}

package com.graduration.Configuration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TopicSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Integer columnCount = jdbcTemplate.queryForObject(
                """
				select count(*) from information_schema.columns
				where table_schema = database()
				and table_name = 'topic'
				and column_name = 'proposed_team_id'
				""",
                Integer.class);
        if (columnCount == null || columnCount == 0) {
            jdbcTemplate.execute("alter table topic add column proposed_team_id bigint null");
        }

        Integer indexCount = jdbcTemplate.queryForObject(
                """
				select count(*) from information_schema.statistics
				where table_schema = database()
				and table_name = 'topic'
				and index_name = 'idx_topic_proposed_team'
				""",
                Integer.class);
        if (indexCount == null || indexCount == 0) {
            jdbcTemplate.execute("create index idx_topic_proposed_team on topic (proposed_team_id)");
        }

        Integer foreignKeyCount = jdbcTemplate.queryForObject(
                """
				select count(*) from information_schema.table_constraints
				where constraint_schema = database()
				and table_name = 'topic'
				and constraint_name = 'fk_topic_proposed_team'
				and constraint_type = 'FOREIGN KEY'
				""",
                Integer.class);
        if (foreignKeyCount == null || foreignKeyCount == 0) {
            jdbcTemplate.execute("alter table topic add constraint fk_topic_proposed_team "
                    + "foreign key (proposed_team_id) references team(id_team) on delete set null");
        }
    }
}

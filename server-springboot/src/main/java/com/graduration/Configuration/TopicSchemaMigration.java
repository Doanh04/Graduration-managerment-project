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
        if (columnCount != null && columnCount > 0) {
            jdbcTemplate
                    .queryForList(
                            "select constraint_name from information_schema.key_column_usage "
                                    + "where table_schema = database() and table_name = 'topic' "
                                    + "and column_name = 'proposed_team_id' and referenced_table_name is not null",
                            String.class)
                    .forEach(constraint ->
                            jdbcTemplate.execute("alter table topic drop foreign key `" + constraint + "`"));
            jdbcTemplate.execute("alter table topic drop column proposed_team_id");
        }

        Integer fileColumnCount = jdbcTemplate.queryForObject(
                """
				select count(*) from information_schema.columns
				where table_schema = database()
				and table_name = 'topic'
				and column_name = 'file_data'
				""",
                Integer.class);
        if (fileColumnCount == null || fileColumnCount == 0) {
            jdbcTemplate.execute("alter table topic add column file_data LONGBLOB null");
        }
    }
}

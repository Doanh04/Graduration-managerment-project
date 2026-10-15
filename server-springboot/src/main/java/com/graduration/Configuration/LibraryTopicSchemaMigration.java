package com.graduration.Configuration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LibraryTopicSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureColumn("file_name", "varchar(255) null");
        ensureColumn("content_type", "varchar(255) null");
        ensureColumn("file_size", "bigint null");
        ensureColumn("file_data", "LONGBLOB null");
    }

    private void ensureColumn(String column, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where table_schema = database() and table_name = 'library_topic' and column_name = ?",
                Integer.class,
                column);
        if (count != null && count == 0) {
            jdbcTemplate.execute("alter table library_topic add column " + column + " " + definition);
        }
    }
}

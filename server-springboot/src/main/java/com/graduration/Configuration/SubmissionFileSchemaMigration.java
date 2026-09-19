package com.graduration.Configuration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** Migrates submission files from obsolete filesystem metadata to a database BLOB. */
@Component
@Order(110)
@RequiredArgsConstructor
public class SubmissionFileSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addColumnIfMissing("file_data", "LONGBLOB NOT NULL");
        dropColumnIfPresent("file_path");
        dropColumnIfPresent("stored_file_name");
    }

    private void addColumnIfMissing(String column, String definition) {
        if (!columnExists(column)) {
            jdbcTemplate.execute("alter table submistion add column `" + column + "` " + definition);
        }
    }

    private void dropColumnIfPresent(String column) {
        if (columnExists(column)) {
            jdbcTemplate.execute("alter table submistion drop column `" + column + "`");
        }
    }

    private boolean columnExists(String column) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where table_schema = database() and table_name = 'submistion' and column_name = ?",
                Integer.class,
                column);
        return count != null && count > 0;
    }
}

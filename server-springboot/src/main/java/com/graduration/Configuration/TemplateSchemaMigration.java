package com.graduration.Configuration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TemplateSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Integer filePathColumnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where table_schema = database() and table_name = 'template' and column_name = 'file_path'",
                Integer.class);
        if (filePathColumnCount != null && filePathColumnCount > 0) {
            // file_data is the canonical storage now; remove the obsolete filesystem path.
            jdbcTemplate.execute("alter table template drop column file_path");
        }
        ensureColumn("template_type", "varchar(64) null");
        ensureColumn("original_file_name", "varchar(255) null");
        ensureColumn("content_type", "varchar(255) null");
        ensureColumn("file_size", "bigint null");
        ensureColumn("file_data", "LONGBLOB null");
        // Các bản ghi cũ chưa có loại được xếp vào nhóm Khác để bộ lọc không bỏ sót.
        jdbcTemplate.update("update template set template_type = 'OTHER' where template_type is null");
    }

    private void ensureColumn(String column, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where table_schema = database() and table_name = 'template' and column_name = ?",
                Integer.class,
                column);
        if (count != null && count == 0) {
            jdbcTemplate.execute("alter table template add column " + column + " " + definition);
        }
    }
}

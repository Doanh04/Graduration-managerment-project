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
        String dataType = jdbcTemplate.queryForObject(
                "select data_type from information_schema.columns "
                        + "where table_schema = database() and table_name = 'template' and column_name = 'file_path'",
                String.class);
        if (dataType != null && !"text".equalsIgnoreCase(dataType)) {
            jdbcTemplate.execute("alter table template modify column file_path text null");
        }
    }
}

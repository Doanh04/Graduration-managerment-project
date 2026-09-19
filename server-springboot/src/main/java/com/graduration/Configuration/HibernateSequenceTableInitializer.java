package com.graduration.Configuration;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** Restores the seed row Hibernate requires in every table-backed ID sequence. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class HibernateSequenceTableInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<String> sequenceTables = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables "
                        + "where table_schema = database() and table_name regexp '_seq$'",
                String.class);
        sequenceTables.forEach(this::seedIfEmpty);
    }

    private void seedIfEmpty(String tableName) {
        String quotedTable = "`" + tableName.replace("`", "``") + "`";
        jdbcTemplate.execute("insert into " + quotedTable + " (next_val) " + "select 1 where not exists (select 1 from "
                + quotedTable + ")");
    }
}

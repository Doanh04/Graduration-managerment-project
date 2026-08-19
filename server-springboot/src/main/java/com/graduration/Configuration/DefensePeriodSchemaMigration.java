package com.graduration.Configuration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DefensePeriodSchemaMigration implements ApplicationRunner {
    JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        String dataType = jdbcTemplate.queryForObject(
                """
				select data_type
				from information_schema.columns
				where table_schema = database()
				and table_name = 'defense_period'
				and column_name = 'status'
				""",
                String.class);

        if (dataType != null && !"varchar".equalsIgnoreCase(dataType)) {
            jdbcTemplate.execute("alter table defense_period modify column status varchar(32) not null");
            log.info("Migrated defense_period.status from {} to VARCHAR(32)", dataType);
        }
    }
}

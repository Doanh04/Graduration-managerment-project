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

        Integer registrationTableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables "
                        + "where table_schema = database() and table_name = 'topic_registration'",
                Integer.class);
        if (registrationTableCount != null && registrationTableCount > 0) {
            String enrollmentNullable = jdbcTemplate.queryForObject(
                    "select is_nullable from information_schema.columns "
                            + "where table_schema = database() and table_name = 'topic_registration' "
                            + "and column_name = 'enrollment_id'",
                    String.class);
            if (!"YES".equalsIgnoreCase(enrollmentNullable)) {
                jdbcTemplate.execute(
                        "alter table topic_registration modify column enrollment_id bigint null");
            }

            // Dữ liệu cũ đã tạo enrollment ELIGIBLE ngay lúc giảng viên gửi đề
            // xuất. Bỏ liên kết này để đề xuất đang chờ duyệt không còn xuất hiện
            // trong danh sách ghi danh; enrollment thật sẽ được tạo khi Admin duyệt.
            jdbcTemplate.update(
                    "update topic_registration tr "
                            + "join topic t on t.id_topic = tr.id_topic "
                            + "set tr.enrollment_id = null "
                            + "where t.category_topic = 'LECTURER' "
                            + "and t.status = 'PENDING_APPROVAL' and tr.status = 'PENDING'");
            jdbcTemplate.update(
                    "delete ge from graduation_enrollment ge "
                            + "left join topic_registration tr on tr.enrollment_id = ge.enrollment_id "
                            + "where ge.status = 'ELIGIBLE' and tr.registration_id is null");
        }
    }
}

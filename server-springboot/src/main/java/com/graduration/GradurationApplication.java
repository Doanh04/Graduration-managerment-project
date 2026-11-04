package com.graduration;

import java.time.ZoneId;
import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GradurationApplication {
    static final String DEFAULT_TIME_ZONE = "Asia/Ho_Chi_Minh";

    public static void main(String[] args) {
        configureDefaultTimeZone();
        SpringApplication.run(GradurationApplication.class, args);
    }

    static void configureDefaultTimeZone() {
        String configuredTimeZone = System.getenv().getOrDefault("APP_TIME_ZONE", DEFAULT_TIME_ZONE);
        ZoneId zoneId = ZoneId.of(configuredTimeZone);
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
        System.setProperty("user.timezone", zoneId.getId());
    }
}

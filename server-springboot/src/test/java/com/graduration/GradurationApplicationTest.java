package com.graduration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneId;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GradurationApplicationTest {
    private final TimeZone originalTimeZone = TimeZone.getDefault();
    private final String originalUserTimeZone = System.getProperty("user.timezone");

    @AfterEach
    void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
        if (originalUserTimeZone == null) {
            System.clearProperty("user.timezone");
        } else {
            System.setProperty("user.timezone", originalUserTimeZone);
        }
    }

    @Test
    void configureDefaultTimeZone_usesVietnamTimeWhenHostRunsUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        System.setProperty("user.timezone", "UTC");

        GradurationApplication.configureDefaultTimeZone();

        assertEquals(GradurationApplication.DEFAULT_TIME_ZONE, ZoneId.systemDefault().getId());
        assertEquals(GradurationApplication.DEFAULT_TIME_ZONE, System.getProperty("user.timezone"));
    }
}

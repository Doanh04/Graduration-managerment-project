package com.graduration.Configuration;

import java.security.SecureRandom;

public final class TemporaryPasswordGenerator {
    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int PASSWORD_LENGTH = 16;

    private TemporaryPasswordGenerator() {}

    public static String generate() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int index = 0; index < PASSWORD_LENGTH; index++) {
            password.append(CHARACTERS.charAt(SECURE_RANDOM.nextInt(CHARACTERS.length())));
        }
        return password.toString();
    }
}

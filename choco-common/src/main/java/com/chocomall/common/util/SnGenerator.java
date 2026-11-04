package com.chocomall.common.util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Human-readable serial numbers such as {@code ORD202610081530451234}. */
public final class SnGenerator {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private SnGenerator() {
    }

    public static String next(String prefix) {
        return prefix + LocalDateTime.now().format(FORMAT) + String.format("%04d", RANDOM.nextInt(10_000));
    }
}

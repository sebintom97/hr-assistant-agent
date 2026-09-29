package com.hrassistant.hrcore.common;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * UUIDv7 generator: 48-bit Unix millisecond timestamp, then random bits (RFC 9562).
 * Same format as Postgres 18's uuidv7(), so ids created in Java and in SQL sort the same way.
 * Java 21 has no built-in v7, and it's ~10 lines, so no library.
 */
public final class Ids {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Ids() {
    }

    public static UUID newId() {
        long millis = System.currentTimeMillis();
        long msb = (millis << 16)                 // 48 bits: timestamp
                | (0x7L << 12)                     //  4 bits: version 7
                | (RANDOM.nextInt() & 0xFFFL);     // 12 bits: random
        long lsb = (RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL)
                | 0x8000000000000000L;             // variant bits "10", 62 bits random
        return new UUID(msb, lsb);
    }
}

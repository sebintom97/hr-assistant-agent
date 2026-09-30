package com.hrassistant.hrcore;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The seed's ids and dates, computed the same way the SQL does, so tests can say
 * SeedIds.of("acme:liam.oconnor") instead of copying UUIDs around.
 */
public final class SeedIds {

    public static final UUID LIAM = of("acme:liam.oconnor");
    public static final UUID SARAH = of("acme:sarah.murphy");
    public static final UUID NIAMH = of("acme:niamh.kelly");
    public static final UUID AISLING = of("acme:aisling.ryan");
    public static final UUID EMMA = of("acme:emma.fitzgerald");
    public static final UUID HANNAH = of("acme:hannah.quinn");
    public static final UUID FIONN = of("acme:fionn.gallagher");
    public static final UUID DECLAN = of("acme:declan.walsh");
    public static final UUID BRIGHTWAVE_SARAH = of("brightwave:sarah.fischer");

    public static final UUID AISLING_PENDING_REQUEST = of("acme:leave:aisling-fresh");
    public static final UUID NIAMH_OWN_REQUEST = of("acme:leave:niamh-own");

    private SeedIds() {
    }

    /** Same as Postgres md5(key)::uuid. */
    public static UUID of(String key) {
        try {
            byte[] md5 = MessageDigest.getInstance("MD5").digest(key.getBytes(StandardCharsets.UTF_8));
            String hex = String.format("%032x", new BigInteger(1, md5));
            return UUID.fromString(hex.replaceFirst("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5"));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Same as the seed's pg_temp.d(n): Monday of the current week plus n days. */
    public static LocalDate d(int offsetDays) {
        return LocalDate.now().with(DayOfWeek.MONDAY).plusDays(offsetDays);
    }
}

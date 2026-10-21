package io.eiaun.shared.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class AgoTest {

    @Test
    public void ago() {
        long timestamp = System.currentTimeMillis();
        Ago ago = new Ago(() -> timestamp);
        assertEquals("moments ago", ago.describe(timestamp));
        assertEquals("moments ago", ago.describe(timestamp - Duration.ofSeconds(2).toMillis()));
        assertEquals("moments from now", ago.describe(timestamp + Duration.ofSeconds(2).toMillis()));
        assertEquals("12 secs ago", ago.describe(timestamp - Duration.ofSeconds(12).toMillis()));
        assertEquals("12 secs from now", ago.describe(timestamp + Duration.ofSeconds(12).toMillis()));
        assertEquals("2 mins ago", ago.describe(timestamp - Duration.ofMinutes(2).toMillis()));
        assertEquals("2 mins from now", ago.describe(timestamp + Duration.ofMinutes(2).toMillis()));
        assertEquals("90 mins ago", ago.describe(timestamp - Duration.ofMinutes(90).toMillis()));
        assertEquals("90 mins from now", ago.describe(timestamp + Duration.ofMinutes(90).toMillis()));
        assertEquals("4 hrs ago", ago.describe(timestamp - Duration.ofHours(4).toMillis()));
        assertEquals("4 hrs from now", ago.describe(timestamp + Duration.ofHours(4).toMillis()));
        assertEquals("12 days ago", ago.describe(timestamp - Duration.ofDays(12).toMillis()));
        assertEquals("12 days from now", ago.describe(timestamp + Duration.ofDays(12).toMillis()));
        assertEquals("3 weeks ago", ago.describe(timestamp - Duration.ofDays(25).toMillis()));
        assertEquals("3 weeks from now", ago.describe(timestamp + Duration.ofDays(25).toMillis()));
        assertEquals("1 mon ago", ago.describe(timestamp - Duration.ofDays(35).toMillis()));
        assertEquals("1 mon from now", ago.describe(timestamp + Duration.ofDays(35).toMillis()));
        assertEquals("2 mons ago", ago.describe(timestamp - Duration.ofDays(75).toMillis()));
        assertEquals("2 mons from now", ago.describe(timestamp + Duration.ofDays(75).toMillis()));
        assertEquals("1 year ago", ago.describe(timestamp - Duration.ofDays(370).toMillis()));
        assertEquals("1 year from now", ago.describe(timestamp + Duration.ofDays(370).toMillis()));
        assertEquals("2 years ago", ago.describe(timestamp - Duration.ofDays(2 * 370).toMillis()));
        assertEquals("2 years from now", ago.describe(timestamp + Duration.ofDays(2 * 370).toMillis()));
    }

}
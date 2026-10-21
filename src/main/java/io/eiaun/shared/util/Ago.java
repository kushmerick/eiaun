package io.eiaun.shared.util;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public class Ago {

    private static final Duration TEN_SECONDS = Duration.ofSeconds(10);
    private static final Duration TWO_MINUTES = Duration.ofMinutes(2);
    private static final Duration TWO_HOURS = Duration.ofHours(2);
    private static final Duration ONE_DAY = Duration.ofDays(1);
    private static final Duration TWO_WEEKS = Duration.ofDays(14);
    private static final Duration ONE_MONTH = Duration.ofDays(30);
    private static final Duration ONE_YEAR = Duration.ofDays(365);

    private final Supplier<Long> clock;

    public Ago() {
        this(System::currentTimeMillis);
    }

    public Ago(Supplier<Long> clock) {
        this.clock = clock;
    }

    public String describe(long timestamp) {
        Duration duration = Duration.between(
                Instant.ofEpochMilli(timestamp),
                Instant.ofEpochMilli(this.clock.get()));
        boolean negative = duration.isNegative();
        String ago = " ago";
        if (negative) {
            duration = duration.negated();
            ago = " from now";
        }
        if (duration.compareTo(TEN_SECONDS) < 0) {
            return "moments" + ago;
        }
        long seconds = duration.getSeconds();
        long secondsPerUnit = 1;
        if (duration.compareTo(TWO_MINUTES) < 0) {
            return smoosh(seconds, secondsPerUnit, "sec") + ago;
        }
        secondsPerUnit *= 60;
        if (duration.compareTo(TWO_HOURS) < 0) {
            return smoosh(seconds, secondsPerUnit, "min") + ago;
        }
        secondsPerUnit *= 60;
        if (duration.compareTo(ONE_DAY) < 0) {
            return smoosh(seconds, secondsPerUnit, "hr") + ago;
        }
        secondsPerUnit *= 24;
        if (duration.compareTo(TWO_WEEKS) < 0) {
            return smoosh(seconds, secondsPerUnit, "day") + ago;
        }
        secondsPerUnit *= 7;
        if (duration.compareTo(ONE_MONTH) < 0) {
            return smoosh(seconds, secondsPerUnit, "week") + ago;
        }
        secondsPerUnit *= 4;
        if (duration.compareTo(ONE_YEAR) < 0) {
            return smoosh(seconds, secondsPerUnit, "mon") + ago;
        }
        secondsPerUnit *= 12;
        return smoosh(seconds, secondsPerUnit, "year") + ago;
    }

    private String smoosh(long seconds, long secondsPerUnit, String unit) {
        long units = seconds / secondsPerUnit;
        return units + " " + unit + (units == 1 ? "" : "s");
    }

}

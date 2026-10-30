package io.eiaun.shared.snapshot;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Each recording is written to a director hierarchy named from "prefixes" of the time when the recording started.
 * This allows ~thousands of recordings per hour without any directory containing a huge number of children.
 * Each snapshot has a unique sequential numeric id, starting from zero, and are written to a directory
 * hierarchy under their recording named by "prefixes" of the id.  Similarly, this allows ~thousands of snapshots
 * per second while keeping the number of children per directory manageable.
 * The full hierarchy looks like this:
 * <pre>
 *  recordings/                              Root path
 *   2026-10-13-09/                          YYYY-MM-dd-HH when this run began
 *    2026-10-13-09-15/                      YYYY-MM-dd-HH-mm when this run began
 *      2026-10-13-09-15-31-672/             Full timestamp when this run began
 *       physics.json                        Substance properties and other physical details
 *       config.json                         Configuration (grid size, initial densities, etc)
 *       organisms.json.XX                   Locations and contents of all organisms at "time = 0"
 *       substances.json.XX                  Locations and contents of all substances at "time = 0"
 *       snapshots/                          Snapshots live here
 *        ABC/                               Top 3 digits of the snapshot id
 *          ABCDEF/                          Top 6 digits of the snapshot id
 *            ABCDEFGHI/                     Top 9 digits of the snapshot id
 *              ABCDEFGHIJK/                 Top 12 digits of the snapshot id
 *                ABCDEFGHIJKLMNO/           Full 15 digit snapshot id
 *                  organism-changes.json    Organism changes that produced this state from the prior state
 *                  substance-changes.json   Substance changes that produced this state from the prior state
 *                  organisms.json.XX        Locations and contents of all organisms (full dumps only)
 *                  substances.json.XX       Locations and contents of all substances (full dumps only)
 *  </pre>
 *  where ABCDEFGHIJKLMNO is the zero-padded 15 digit snapshot id, and XX is the compression algorithm
 *  (see {@link io.eiaun.shared.config.Config#SNAPSHOT_COMPRESSION}).
 */
@Slf4j
public class FileSnapshotManipulator {

    protected static final DateTimeFormatter YMDH = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH");
    protected static final DateTimeFormatter YMDHM = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm");
    protected static final DateTimeFormatter YMDHMSS = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm-ss-SSS");

    protected static final int SNAPSHOT_ID_DIGITS_PER_SEGMENT = 3; // one thousand files per directory
    protected static final int SNAPSHOT_ID_SEGMENTS = 5; // 10^(3*5) = one quadrillion ids sort alphabetically
    protected static final String[] SNAPSHOT_ID_FORMAT = new String[SNAPSHOT_ID_SEGMENTS];
    protected static final long[] SNAPSHOT_ID_DIVISOR = new long[SNAPSHOT_ID_SEGMENTS];
    static {
        for (int i = 0; i < SNAPSHOT_ID_SEGMENTS; i++) {
            SNAPSHOT_ID_DIVISOR[i] = Math.powExact(10L, SNAPSHOT_ID_DIGITS_PER_SEGMENT * (SNAPSHOT_ID_SEGMENTS - i - 1));
            SNAPSHOT_ID_FORMAT[i] = "%0" + (i + 1) * SNAPSHOT_ID_DIGITS_PER_SEGMENT + "d";
        }
    }

    protected static final String ROOT = "recordings";
    protected static final String DOT_JSON = ".json";
    protected static final String PHYSICS = "physics" + DOT_JSON;
    protected static final String CONFIG = "config" + DOT_JSON;
    protected static final String SNAPSHOTS = "snapshots";
    protected static final String DOT_JSON_DOT = DOT_JSON + ".";
    protected static final String ORGANISMS_DOT = "organisms" + DOT_JSON_DOT;
    protected static final String ORGANISM_CHANGES = "organism-changes" + DOT_JSON;
    protected static final String SUBSTANCES_DOT = "substances" + DOT_JSON_DOT;
    protected static final String SUBSTANCE_CHANGES = "substance-changes" + DOT_JSON;

    protected static long getSnapshotId(Path path) {
        // sample:
        // recordings/2026-10-08-21/2026-10-08-21-34/2026-10-08-21-34-26-815/snapshots/ABC/ABCDEF/ABCDEFGHI/ABCDEFGHIJKL/ABCDEFGHIJKLMNO
        // where ABCDEFGHIJKLMNO is the (SNAPSHOT_ID_SEGMENTS*SNAPSHOT_ID_DIGITS_PER_SEGMENT) = 15 digit zero-padded
        // snapshot id.
        return Long.parseLong(path.getFileName().toString());
    }

    protected static long getRecordingTimestamp(Path path, DateTimeFormatter format) {
        // sample:
        // recordings/2026-10-08-21/2026-10-08-21-34/2026-10-08-21-34-26-815
        return LocalDateTime.parse(path.getFileName().toString(), format)
                .atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
    }

    protected static boolean isRecordingTimestamp(Path path, DateTimeFormatter format) {
        try {
            getRecordingTimestamp(path, format);
            return true;
        } catch (DateTimeParseException _) {
            return false;
        }
    }

    static Path makeSnapshotPath(Path recording, long snapshotId) {
        String[] segments = new String[SNAPSHOT_ID_SEGMENTS];
        for (int i = 0; i < SNAPSHOT_ID_SEGMENTS; i++) {
            segments[i] = String.format(SNAPSHOT_ID_FORMAT[i], snapshotId / SNAPSHOT_ID_DIVISOR[i]);
        }
        return recording.resolve(SNAPSHOTS, segments);
    }

}

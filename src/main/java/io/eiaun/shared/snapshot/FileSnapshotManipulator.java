package io.eiaun.shared.snapshot;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Snapshots are written to files like:
 * <pre>
 *  recordings/                                                                Root path
 *   2026-10-13-09/                                     YYYY-MM-dd-HH when this run began
 *    2026-10-13-09-15/                                 YYYY-MM-dd-HH-mm when this run began
 *      2026-10-13-09-15-31-672/                        Full timestamp when this run began
 *       physics.json                                   Substance properties and other physical details
 *       config.json                                    Configuration (grid size, initial densities, etc)
 *       organisms.json.XX                              Locations and contents of all organisms at "time = 0"
 *       substances.json.XX                             Locations and contents of all substances at "time = 0"
 *       snapshots/                                     Snapshots live here
 *        2026-10-14-10/                                YYYY-MM-dd-HH when this snapshot was taken
 *          2026-10-14-10-51/                           YYYY-MM-dd-HH-mm when this snapshot was taken
 *            2026-10-14-10-51-17-186/                  Full timestamp when this snapshot was taken
 *              0000023782371-2026-10-14-10-51-17-186/  Id & full Timestamp of this snapshot
 *                organism-changes.json                 Organism changes that produced this state from the prior state
 *                substance-changes.json                Substance changes that produced this state from the prior state
 *                organisms.json.XX                     Locations and contents of all organisms (full dumps only)
 *                substances.json.XX                    Locations and contents of all substances (full dumps only)
 *  </pre>
 *  where XX is the compression algorithm (see {@link io.eiaun.shared.config.Config#SNAPSHOT_COMPRESSION}).
 */
@Slf4j
public class FileSnapshotManipulator {

    protected static final DateTimeFormatter YMDH = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH");
    protected static final DateTimeFormatter YMDHM = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm");
    protected static final DateTimeFormatter YMDHMSS = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm-ss-SSS");
    protected static final int SNAPSHOT_ID_WIDTH = 13; // pad ids with enough 0's so that a trillion sorts alphabetically
    protected static final String SNAPSHOT_ID_FORMAT = "%0" + SNAPSHOT_ID_WIDTH + "d";
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

    protected static Optional<Pair<Long, Long>> parseSnapshot(Path path) {
        // sample:
        // recordings/2026-10-08-21/2026-10-08-21-34/2026-10-08-21-34-26-815/snapshots/2026-10-08-21/2026-10-08-21-35/2026-10-08-21-35-05-848/0000000001129-2026-10-08-21-35-05-848
        String name = path.getFileName().toString();
        int dash = name.indexOf('-');
        if (dash == SNAPSHOT_ID_WIDTH) {
            long id = Long.parseLong(name.substring(0, dash));
            name = name.substring(dash + 1);
            Optional<Long> timestamp = parse(Path.of(name), YMDHMSS);
            return timestamp.map(t -> Pair.of(id, t));
        }
        return Optional.empty();
    }

    protected static Optional<Long> parse(Path path, DateTimeFormatter format) {
        // sample:
        // recordings/2026-10-08-21/2026-10-08-21-34/2026-10-08-21-34-26-815
        String name = path.getFileName().toString();
        try {
            long timestamp = LocalDateTime.parse(name, format)
                    .atZone(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli();
            return Optional.of(timestamp);
        } catch (DateTimeParseException failure) {
            log.warn("Failure while parsing `{}` with `{}`", path, format, failure);
        }
        return Optional.empty();
    }

}

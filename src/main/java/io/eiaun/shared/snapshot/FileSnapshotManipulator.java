package io.eiaun.shared.snapshot;

import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;

/**
 * Snapshots are written to files like:
 * <pre>
 *  recordings/                                                              Root path
 *   2026-10-13-09/                                                          YYYY-MM-dd-HH when this run began
 *    2026-10-13-09-15/                                                      YYYY-MM-dd-HH-mm when this run began
 *      2026-10-13-09-15-31-672/                                             Full timestamp when this run began
 *       physics.json                                                        Substance properties and other physical details
 *       config.json                                                         Configuration (grid size, initial densities, etc)
 *       snapshots/                                                          Snapshots live here
 *        2026-10-14-10/                                                     YYYY-MM-dd-HH when this snapshot was taken
 *          2026-10-14-10-51/                                                YYYY-MM-dd-HH-mm when this snapshot was taken
 *            2026-10-14-10-51-17-186/                                       Full timestamp when this snapshot was taken
 *              0000023782371-2026-10-14-10-51-17-186/                       ID & full Timestamp of this snapshot
 *              0000023782371-2026-10-14-10-51-17-186.organisms.json.XX      Locations and contents of all organisms
 *              0000023782371-2026-10-14-10-51-17-186.substances.json.XX     Locations and contents of all substances
 *              0000023782371-2026-10-14-10-51-17-186.organism-changes.json  Organism changes that produced this state from the prior state
 *              0000023782371-2026-10-14-10-51-17-186.substance-changes.json Substance changes that produced this state from the prior state
 *  </pre>
 *  where XX is the compression algorithm (see {@link io.eiaun.shared.config.Config#SNAPSHOT_COMPRESSION}).
 */
@Slf4j
public class FileSnapshotManipulator {

    protected static final SimpleDateFormat YMDH_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH");
    protected static final SimpleDateFormat YMDHM_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
    protected static final SimpleDateFormat YMDHMSS_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
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

}

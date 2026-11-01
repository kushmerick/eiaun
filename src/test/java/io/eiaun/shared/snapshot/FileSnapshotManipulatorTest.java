package io.eiaun.shared.snapshot;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static io.eiaun.shared.snapshot.FileSnapshotManipulator.SNAPSHOTS;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FileSnapshotManipulatorTest {

    @Test
    void canFormatSnapshotPath() {
        Path recording = Path.of("test123");
        assertEquals(
                recording.resolve(
                        SNAPSHOTS,
                        "102",
                        "102304",
                        "102304506",
                        "102304506708",
                        "102304506708901"),
                FileSnapshotManipulator.makeSnapshotPath(recording, 102_304_506_708_901L));
    }

}
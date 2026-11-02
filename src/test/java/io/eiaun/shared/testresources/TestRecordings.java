package io.eiaun.shared.testresources;

import java.nio.file.Path;

public class TestRecordings {

    public static final Path RECORDINGS = Path.of("src/test/resources/test-recordings");

    // we assume there is exactly one test recording, traverse the filesystem to find it.
    public static Path RECORDING;
    static {
        RECORDING = RECORDINGS;
        for (int i = 0; i < 3; i++) {
            RECORDING = RECORDING.resolve(RECORDING.toFile().list()[0]);
        }
    }

}

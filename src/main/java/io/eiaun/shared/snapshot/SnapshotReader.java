package io.eiaun.shared.snapshot;

import io.eiaun.shared.util.DirectoryWatcher;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Path;

public interface SnapshotReader {

    AutoCloseable watchRecordings(DirectoryWatcher.Listener listener) throws IOException;

}

package io.eiaun.shared.snapshot;

import io.eiaun.shared.util.DirectoryWatcher;

import java.io.IOException;
import java.nio.file.Path;

public interface SnapshotReader {

    AutoCloseable watchRecordings(DirectoryWatcher.Listener listener) throws IOException;

    AutoCloseable watchSnapshots(Path recording, DirectoryWatcher.Listener listener) throws IOException;

}

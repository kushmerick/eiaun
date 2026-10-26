package io.eiaun.viz.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.svg.Svg;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.eiaun.shared.snapshot.FileSnapshots;
import io.eiaun.shared.snapshot.SnapshotReader;
import io.eiaun.shared.snapshot.Snapshots;
import io.eiaun.shared.util.Ago;
import io.eiaun.shared.util.DirectoryWatcher;
import io.eiaun.viz.atlas.Atlas;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Route("")
@PageTitle("VIZ: Home")
@Component
@Slf4j
public class HomeView extends VerticalLayout {

    private static final Ago AGO = new Ago();
    private static final DateTimeFormatter YMDHM = DateTimeFormatter.ofPattern("uuuu-MM-dd @ HH:mm");
    private static final DateTimeFormatter YMDHMS = DateTimeFormatter.ofPattern("uuuu-MM-dd @ HH:mm:ss");

    private final SnapshotReader snapshotReader;
    private final Set<Path> recordings = new TreeSet<>(Comparator.reverseOrder());
    private final Set<Path> snapshots = new TreeSet<>(Comparator.reverseOrder());
    private final ListBox<Path> recordingsMenu = new ListBox<>();
    private Snapshots snaps = null;
    private final ListBox<Path> snapshotsMenu = new ListBox<>();
    private final Paragraph snapshotDisplay = new Paragraph();
    private AutoCloseable recordingsCloser = null;
    private AutoCloseable snapshotsCloser = null;
    private Atlas atlas;

    @Autowired
    public HomeView(
            SnapshotReader snapshotReader
    ) {
        this.snapshotReader = snapshotReader;
        this.recordingsMenu.addClassName("home-recordings");
        this.recordingsMenu.setWidth("50%");
        this.recordingsMenu.setHeight(25, Unit.CH);
        this.recordingsMenu.setItemLabelGenerator(recording -> {
            long timestamp = FileSnapshots.getRecordingTimestamp(recording);
            return YMDHM.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC))
                    + " (" + AGO.describe(timestamp) + ")";
        });
        this.snapshotsMenu.addClassName("home-snapshots");
        this.snapshotsMenu.setWidth("50%");
        this.snapshotsMenu.setHeight(25, Unit.CH);
        this.snapshotsMenu.setItemLabelGenerator(snapshot -> {
            if (this.snaps == null) {
                // TODO: But if it does, getSnapshotIndexAndTimestamp could be made static
                return "this should never happen";
            }
            Pair<Long, Long> indexTimestamp = this.snaps.getSnapshotIndexAndTimestamp(snapshot);
            long index = indexTimestamp.getLeft();
            long timestamp = indexTimestamp.getRight();
            return index + ": " + YMDHMS.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC)) + " (" + AGO.describe(timestamp) + ")";
        });
        HorizontalLayout horizontal = new HorizontalLayout();
        horizontal.setWidth("100%");
        horizontal.add(recordingsMenu, snapshotsMenu);
        add(horizontal);
        add(this.snapshotDisplay);
    }

    @Override
    protected void onAttach(AttachEvent event) {
        recordingsMenu.addValueChangeListener(e ->
                updateSnapshots(e.getUI(), e.getValue()));
        snapshotsMenu.addValueChangeListener(e ->
                updateSnapshotDisplay(e.getUI(), e.getValue()));
        UI ui = event.getUI();
        try {
            this.recordingsCloser = snapshotReader.watchRecordings(
                    DirectoryWatcher.Listener.ifCreate(recording ->
                            ui.access(() -> {
                                this.recordings.add(recording);
                                Path selected = recordingsMenu.getValue();
                                this.recordingsMenu.setItems(recordings);
                                if (selected != null) this.recordingsMenu.setValue(selected);
                            })));
        } catch (IOException failure) {
            throw new UncheckedIOException("Failure while watching recordings", failure);
        }
    }

    private void updateSnapshots(UI ui, Path recording) {
        if (recording == null) {
            return; // seems weird  but we've seen it in the wild
        }
        try {
            this.snapshotDisplay.setText(null);
            if (this.snapshotsCloser != null) {
                this.snapshotsCloser.close();
                this.snapshotsCloser = null;
            }
            this.snapshots.clear();
            this.snaps = FileSnapshots.of(recording);
            if (this.atlas != null) {
                remove(this.atlas.get());
            }
            this.atlas = new Atlas(this.snaps);
            ui.access(() -> snapshotsMenu.setItems(Collections.emptyList()));
            this.snapshotsCloser = this.snapshotReader.watchSnapshots(
                    recording,
                    DirectoryWatcher.Listener.ifCreate(snapshot ->
                            ui.access(() -> {
                                snapshots.add(snapshot);
                                snapshotsMenu.setItems(snapshots);
                            })));
        } catch (Exception failure) {
            log.warn("Failure while updating snapshots", failure);
        }
    }

    private void updateSnapshotDisplay(UI ui, Path snapshot) {
        if (snapshot == null) {
            return; // seems weird but we've seen it in the wild
        }
        try {
            Pair<Long,Long> indexTimestamp = this.snaps.getSnapshotIndexAndTimestamp(snapshot);
            int organismChanges = this.snaps.getSnapshotOrganismChanges(snapshot).size();
            int substanceChanges = this.snaps.getSnapshotSubstanceChanges(snapshot).size();
            int organisms = Optional.ofNullable(this.snaps.getOrganisms()).map(o -> o.firstIndices().size()).orElse(-123);
            int substances = Optional.ofNullable(this.snaps.getSubstances()).map(s -> s.firstIndices().size()).orElse(-123);
            String[] display = {
                    String.format("%s (%s) %s organisms (%s changes); %s substance (%s changes)",
                            indexTimestamp.getLeft(),
                            AGO.describe(indexTimestamp.getRight()),
                            organisms, organismChanges, substances, substanceChanges)
            };
            ui.access(() -> this.snapshotDisplay.setText(display[0]));
            // Atlas!
            ui.access(() -> remove(atlas.get())); // is this needed / is there a better/correct way to refresh?
            Snapshots.StepToResult result = this.atlas.stepTo(snapshot);
            if (result.dirty()) {
                Notification.show(result.dirt());
            }
            Svg theAtlas = atlas.get();
            theAtlas.setWidth(30, Unit.CM);
            ui.access(() -> add(theAtlas));
        } catch (Exception failure) {
            Notification.show("Oops! " + failure.getMessage());
            log.warn("Failure while updating snapshot display", failure);
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (this.recordingsCloser != null) {
            try {
                this.recordingsCloser.close();
            } catch (Exception failure) {
                log.warn("Closing recordings", failure);
            }
            this.recordingsCloser = null;
        }
        if (this.snapshotsCloser != null) {
            try {
                this.snapshotsCloser.close();
            } catch (Exception failure) {
                log.warn("Closing snapshots", failure);
            }
            this.snapshotsCloser = null;
        }
    }

}

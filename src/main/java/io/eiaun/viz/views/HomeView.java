package io.eiaun.viz.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.slider.DecimalSlider;
import com.vaadin.flow.component.svg.Svg;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.eiaun.shared.snapshot.FileSnapshots;
import io.eiaun.shared.snapshot.SnapshotReader;
import io.eiaun.shared.snapshot.Snapshots;
import io.eiaun.shared.util.Ago;
import io.eiaun.shared.util.DirectoryWatcher;
import io.eiaun.shared.util.Functionals;
import io.eiaun.viz.atlas.Atlas;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Route("")
@PageTitle("VIZ: Home")
@Component
@Slf4j
public class HomeView extends VerticalLayout {

    private static final Ago AGO = new Ago();
    private static final DateTimeFormatter YMDHM = DateTimeFormatter.ofPattern("uuuu-MM-dd @ HH:mm");
    private static final DateTimeFormatter YMDHMS = DateTimeFormatter.ofPattern("uuuu-MM-dd @ HH:mm:ss");
    private static final double LN_MIN_PLAY_SLEEP = Math.log(Duration.ofMillis(200).toMillis());
    private static final double LN_MAX_PLAY_SLEEP = Math.log(Duration.ofSeconds(3).toMillis());

    private final ExecutorService executor;
    private final SnapshotReader snapshotReader;
    private final Set<Path> recordings;
    private final ListBox<Path> recordingsPicker;
    private final Paragraph recordingDetails;
    private final Paragraph snapshotDetails;
    private final HorizontalLayout buttons;
    private final Button resetButton;
    private final Button back10Button;
    private final Button backButton;
    private final Button playButton;
    private final Button pauseButton;
    private final Button forwardButton;
    private final Button forward10Button;
    private final Button toEndButton;
    private final DecimalSlider speedSlider;
    private Future<?> loadingRecording = null;
    private Snapshots snapshots;
    private AutoCloseable recordingsCloser = null;
    private Future<?> playing = null;
    private Atlas atlas = null;

    @Autowired
    public HomeView(
            ExecutorService executor,
            SnapshotReader snapshotReader
    ) {
        this.executor = executor;
        this.snapshotReader = snapshotReader;
        this.recordings = new TreeSet<>(Comparator.reverseOrder());
        this.recordingsPicker = new ListBox<>();
        this.recordingDetails = new Paragraph();
        this.snapshotDetails = new Paragraph();
        this.buttons = new HorizontalLayout();
        this.resetButton = new Button(VaadinIcon.REPLY.create());
        this.resetButton.setTooltipText("Reset to the first snapshot");
        this.back10Button = new Button(VaadinIcon.FAST_BACKWARD.create());
        this.back10Button.setTooltipText("Back 10 snapshots");
        this.backButton = new Button(VaadinIcon.BACKWARDS.create());
        this.backButton.setTooltipText("Back one snapshot");
        this.playButton = new Button(VaadinIcon.PLAY.create());
        this.playButton.setTooltipText("Play");
        this.pauseButton = new Button(VaadinIcon.PAUSE.create());
        this.pauseButton.setTooltipText("Pause");
        this.forwardButton = new Button(VaadinIcon.FORWARD.create());
        this.forwardButton.setTooltipText("Forward one snapshot");
        this.forward10Button = new Button(VaadinIcon.FAST_FORWARD.create());
        this.forward10Button.setTooltipText("Forward 10 snapshots");
        this.toEndButton = new Button(VaadinIcon.SHARE.create());
        this.toEndButton.setTooltipText("Forward to the last snapshot");
        this.speedSlider = new DecimalSlider("Speed", LN_MIN_PLAY_SLEEP, LN_MAX_PLAY_SLEEP); // TODO: Can't get tooltip to display!?
    }

    @Override
    protected void onAttach(AttachEvent event) {
        HorizontalLayout upper = new HorizontalLayout();
        upper.add(this.recordingsPicker);
        VerticalLayout upperRight = new VerticalLayout();
        this.recordingDetails.setWidth("100%");
        upperRight.add(this.recordingDetails);
        this.buttons.add(this.resetButton);
        this.buttons.add(this.back10Button);
        this.buttons.add(this.backButton);
        this.buttons.add(this.playButton);
        this.buttons.add(this.pauseButton);
        this.buttons.add(this.forwardButton);
        this.buttons.add(this.forward10Button);
        this.buttons.add(this.toEndButton);
        this.speedSlider.setStep((LN_MAX_PLAY_SLEEP - LN_MIN_PLAY_SLEEP) / 50);
        this.speedSlider.setValue((LN_MAX_PLAY_SLEEP + LN_MIN_PLAY_SLEEP) / 2);
        this.buttons.add(this.speedSlider);
        this.buttons.setWidth("50%");
        upperRight.add(buttons);
        this.snapshotDetails.setWidth("100%");
        upperRight.add(this.snapshotDetails);
        upper.add(upperRight);
        upper.setWidth("100%");
        add(upper);
        this.recordingsPicker.addClassName("home-recordings");
        this.recordingsPicker.setWidth("50%");
        this.recordingsPicker.setHeight(25, Unit.CH);
        this.recordingsPicker.setItemLabelGenerator(recording -> {
            long timestamp = FileSnapshots.getRecordingTimestamp(recording);
            return YMDHM.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC))
                    + " (" + AGO.describe(timestamp) + ")";
        });
        this.buttons.setVisible(false);
        this.recordingsPicker.addValueChangeListener(e ->
                pickRecording(e.getUI(), e.getValue()));
        try {
            this.recordingsCloser = this.snapshotReader.watchRecordings(
                    DirectoryWatcher.Listener.ifCreate(recording ->
                            event.getUI().access(() -> {
                                this.recordings.add(recording);
                                Path current = recordingsPicker.getValue();
                                this.recordingsPicker.setItems(recordings);
                                if (current != null) this.recordingsPicker.setValue(current);
                            })));
        } catch (IOException failure) {
            throw new UncheckedIOException("Failure while watching recordings", failure);
        }
        this.resetButton.addClickListener(e -> pickRecording(e.getUI(), this.recordingsPicker.getValue()));
        this.back10Button.addClickListener(e -> notifyOnFailure(bumpIfPaused(-10, e.getUI()), "Can't go so far backwards."));
        this.backButton.addClickListener(e -> notifyOnFailure(bumpIfPaused(-1, e.getUI()), "Can't go backwards"));
        this.playButton.addClickListener(e -> play(e.getUI()));
        this.pauseButton.addClickListener(e -> pause(e.getUI(), true));
        this.forwardButton.addClickListener(e -> notifyOnFailure(bumpIfPaused(0, e.getUI()), "Can't go forwards"));
        this.forward10Button.addClickListener(e -> notifyOnFailure(bumpIfPaused(+10, e.getUI()),  "Can't go so far forwards"));
        this.toEndButton.addClickListener(_ -> notifyOnFailure(false, "Not yet implemented!"));
    }

    private void notifyOnFailure(boolean success, String message) {
        if (!success) {
            Notification.show(message);
        }
    }

    private void pickRecording(UI ui, Path recording) {
        if (recording == null) {
            return; // seems weird but we've seen it in the wild
        }
        stopLoadingRecording();
        // push the heavy lifting to another thread
        this.loadingRecording = this.executor.submit(() -> {
            pause(ui, false);
            this.snapshots = FileSnapshots.of(recording);
            ui.access(() -> {
                try {
                    if (this.atlas != null) {
                        remove(this.atlas.get());
                    }
                    this.buttons.setVisible(false);
                    this.snapshotDetails.setVisible(false);
                    this.recordingDetails.setText(
                            formatTimestamp(this.snapshots.getRecordingTimestamp(), YMDHM) + ": " +
                                    "Grid " + this.snapshots.getConfig().getGrid() +
                                    "; Substances " + this.snapshots.getPhysics().getSubstanceSpecs().size());
                    this.buttons.setVisible(true);
                    this.atlas = new Atlas(this.snapshots);
                    Svg map = this.atlas.get();
                    map.setWidth("100%");
                    add(map);
                } catch (Exception failure) {
                    log.warn("Failure while picking recording", failure);
                }
            });
            // TODO: This is quite slow, need to explicitly check for interruption in the below loops.
            this.snapshots.getOrganisms().forEach(
                    (lat, lon, organism) -> ui.access(() -> this.atlas.addOrganism(lat, lon, organism)));
            this.snapshots.getSubstances().forEach(
                    (lat, lon, substance) -> ui.access(() -> this.atlas.addSubstance(lat, lon, substance)));
        });
    }

    private boolean bumpIfPaused(int delta, UI ui) {
        if (this.playing != null && !this.playing.isDone()) {
            // don't bump while playing.  probably hard to address all race conditions.
            // instead, the user must first pause.
            return true;
        }
        return bump(delta, ui);
    }

    private boolean bump(int delta, UI ui) {
        this.snapshots.bumpNextSnapshotIndex(delta);
        boolean[] success = { true };
        try {
            ui.access(() -> {
                Snapshots.StepToResult result = this.snapshots.stepToNextSnapshot(
                        this.atlas::addOrganism,
                        this.atlas::removeOrganism,
                        this.atlas::addSubstance,
                        this.atlas::removeSubstance);
                if (result.dirty()) {
                    Notification.show("Oops: " + result.dirt());
                }
                if (result.snapshot() == null) {
                    success[0] = false;
                } else {
                    Pair<Long, Long> indexTimestamp = this.snapshots.getSnapshotIndexAndTimestamp();
                    this.snapshotDetails.setVisible(true);
                    this.snapshotDetails.setText("Snapshot " + indexTimestamp.getLeft() + ": " +
                            formatTimestamp(indexTimestamp.getRight(), YMDHMS));
                }
            });
        } catch (Exception failure) {
            Notification.show("Can't step to next snapshot: " + failure.getMessage());
            log.warn("Failure while stepping to next snapshot", failure);
            success[0] = false;
        }
        return success[0];
    }

    private void stopLoadingRecording() {
        if (this.loadingRecording != null) {
            this.loadingRecording.cancel(true);
            this.loadingRecording = null;
        }
    }

    private boolean isLoadingRecording() {
        return this.loadingRecording != null && !this.loadingRecording.isDone();
    }

    private void pause(UI ui, boolean focusPause) {
        if (this.playing != null) {
            if (focusPause) {
                ui.access(this.pauseButton::focus);
            }
            this.playing.cancel(true);
            this.playing = null;
        }
    }

    private void play(UI ui) {
        if (isLoadingRecording()) return;
        if (playing != null) return;
        this.playing = this.executor.submit(() -> {
            ui.access(this.playButton::focus);
            boolean more;
            do {
                log.info("Playing: Sleeping");
                try {
                    long sleep = Math.round(Math.exp(this.speedSlider.getValue()));
                    Thread.sleep(sleep);
                } catch (InterruptedException _) {
                    return;
                }
                log.info("Playing: Bumping");
                more = bump(0, ui);
            } while (more);
            log.info("Playing: Done");
        });
    }

    private String formatTimestamp(long timestamp, DateTimeFormatter format) {
        return  format.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC)) +
                " (" + AGO.describe(timestamp) + ")";
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (this.recordingsCloser != null) {
            Functionals.run(this.recordingsCloser::close);
            this.recordingsCloser = null;
        }
    }

}

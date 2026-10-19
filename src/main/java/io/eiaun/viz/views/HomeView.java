package io.eiaun.viz.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.eiaun.shared.snapshot.SnapshotReader;
import io.eiaun.shared.util.DirectoryWatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Route("")
@PageTitle("VIZ: Home")
@Component
@Slf4j
public class HomeView extends VerticalLayout {

    @Autowired
    public HomeView(
            SnapshotReader snapshotReader
    ) throws IOException {
        add(new Paragraph("Recordings"));
        UI ui = UI.getCurrent();
        Set<Path> recordings = new TreeSet<>(Comparator.reverseOrder());
        ListBox<Path> recordingsMenu = new ListBox<>();
        recordingsMenu.addClassName("home-recordings-menu");
        recordingsMenu.setHeight(25, Unit.CH);
        add(recordingsMenu);
        Paragraph selectedRecording = new Paragraph("nothing selected");
        add(selectedRecording);
        recordingsMenu.addValueChangeListener(event ->
                Optional.ofNullable(event.getValue()).map(Path::toString).ifPresent(recording ->
                        ui.access(() -> selectedRecording.setText(recording))));
        AutoCloseable _ = snapshotReader.watchRecordings(
                DirectoryWatcher.Listener.ifCreate(recording -> {
                    log.info("Recording: {}", recording);
                    recordings.add(recording);
                    ui.access(() -> recordingsMenu.setItems(recordings));
                }));
    }

}

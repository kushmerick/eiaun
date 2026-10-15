package io.eiaun.viz.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.eiaun.shared.snapshot.SnapshotReader;
import io.eiaun.viz.VIZ;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Route("")
@PageTitle("VIZ: Home")
@Component
public class HomeView extends VerticalLayout {

    @Autowired
    public HomeView(
            SnapshotReader snapshotReader
    ) throws IOException {
        add(new Paragraph("Recordings"));
        Span recordingsSpan = new Span();
        add(recordingsSpan);
        Set<Path> recordings = new HashSet<>();
        UI ui =  UI.getCurrent();
        AutoCloseable recordingsCloser = snapshotReader.watchRecordings(
                ((verb, child) -> {
                    switch (verb) {
                        case CREATE, MODIFY -> recordings.add(child);
                        case DELETE -> recordings.remove(child);
                    }
                    List<String> sequence = recordings.stream()
                                    .map(Path::toString)
                                            .sorted()
                                                    .toList();
                    ui.access(() -> recordingsSpan.setText(sequence.toString()));
                })
        );
        this.addDetachListener(event -> {
            try {
                recordingsCloser.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

}

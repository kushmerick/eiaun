package io.eiaun.viz.atlas;

import com.vaadin.flow.component.svg.Svg;
import com.vaadin.flow.component.svg.elements.Circle;
import com.vaadin.flow.component.svg.elements.Rect;
import com.vaadin.flow.component.svg.elements.SvgElement;
import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.physics.Location;
import io.eiaun.shared.physics.Substance;
import io.eiaun.shared.snapshot.Snapshots;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class Atlas implements Supplier<Svg> {

    private static final RandomStringUtils RANDOM_STRINGS = RandomStringUtils.secure();

    private final int ORGANISM_RADIUS = 5;
    private final String ORGANISM_COLOR = "green";

    private final int SUBSTANCE_SIZE = 5;
    private final String SUBSTANCE_COLOR = "orange";

    private final Snapshots snapshots;
    private final Svg atlas;

    // key --> icon
    // for organisms, key = organism's id as string
    // for substance, key = substance's id + lat + lon
    private final Map<String, SvgElement> icons;

    public Atlas(Snapshots snapshots) {
        this.snapshots = snapshots;
        this.atlas = new Svg();
        this.icons = new HashMap<>();
    }

    public Svg get() {
        return this.atlas;
    }

    public Snapshots.StepToResult stepTo(Path snapshot) {
        Snapshots.StepToResult result = this.snapshots.stepTo(snapshot,
                this::addOrganism,
                this::removeOrganism,
                this::addSubstance,
                this::removeSubstance);
        if (result.dirty()) {
            return result;
        }
        if (this.atlas.getSvgElements().isEmpty()) {
            // snapshots.stepTo invokes callbacks only for changes, so need to explicitly populate for the first time
            this.snapshots.getOrganisms().forEach(this::addOrganism);
            this.snapshots.getSubstances().forEach(this::addSubstance);
        }
        return Snapshots.StepToResult.CLEAN;
    }

    private void addOrganism(Location location, Organism organism) {
        addOrganism(location.getLat(), location.getLon(), organism);
    }

    private void addOrganism(int lat, int lon, Organism organism) {
        String id = String.valueOf(organism.getId());
        Circle icon = new Circle(id, ORGANISM_RADIUS);
        icon.setFillColor(ORGANISM_COLOR);
        addIcon(icon, lat, lon);
    }

    private void addSubstance(Location location, Substance substance) {
        addSubstance(location.getLat(), location.getLon(), substance);
    }

    private void addSubstance(int lat, int lon, Substance substance) {
        String id = substanceIconId(lat, lon, substance);
        Rect icon = new Rect(id, SUBSTANCE_SIZE, SUBSTANCE_SIZE);
        icon.setFillColor(SUBSTANCE_COLOR);
        addIcon(icon, lat, lon);
    }

    private void addIcon(SvgElement icon, int lat, int lon) {
        icon.move(lat, lon);
        this.atlas.add(icon);
        this.icons.put(icon.getId(), icon);
    }

    private void removeOrganism(Location ignored, Organism organism) {
        removeIcon(String.valueOf(organism.getId()));
    }

    private void removeSubstance(Location location, Substance substance) {
        removeIcon(substanceIconId(location.getLat(), location.getLon(), substance));
    }

    private void removeIcon(String id) {
        this.atlas.remove(this.icons.get(id));
        this.icons.remove(id);
    }

    private String substanceIconId(int lat, int lon, Substance substance) {
        return substance.getId() + "@" + lat + "-" + lon;
    }

}

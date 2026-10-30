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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Atlas implements Supplier<Svg> {

    private static final RandomStringUtils RANDOM_STRINGS = RandomStringUtils.secure();

    private final int ORGANISM_RADIUS = 2;
    private final String ORGANISM_COLOR = "green";

    private final int SUBSTANCE_SIZE = 4;
    private final String SUBSTANCE_COLOR = "orange";

    private final int UNIT = Math.max(ORGANISM_RADIUS, SUBSTANCE_SIZE);
    private final int JIGGLE = 1; // offset substance so looks nicer in case it is co-located with an organism

    private final Svg atlas;

    // key --> icon
    // for organisms, key = organism's id as string
    // for substance, key = substance's id + lat + lon
    private final Map<String, SvgElement> icons;

    public Atlas(Snapshots snapshots) {
        int grid = snapshots.getConfig().getGrid();
        this.atlas = new Svg();
        int size = (grid + 1) * UNIT + JIGGLE;
        this.atlas.viewbox(0, 0, size, size);
        this.icons = new HashMap<>();
    }

    public Svg get() {
        return this.atlas;
    }

    public boolean isEmpty() {
        return this.atlas.getSvgElements().isEmpty();
    }

    public void addOrganism(Location location, Organism organism) {
        addOrganism(location.getLat(), location.getLon(), organism);
    }

    public void addOrganism(int lat, int lon, Organism organism) {
        String id = String.valueOf(organism.getId());
        Circle icon = new Circle(id, ORGANISM_RADIUS);
        icon.setFillColor(ORGANISM_COLOR);
        addIcon(icon, lat, lon);
    }

    public void addSubstance(Location location, Substance substance) {
        addSubstance(location.getLat(), location.getLon(), substance);
    }

    public void addSubstance(int lat, int lon, Substance substance) {
        String id = substanceIconId(lat, lon, substance);
        Rect icon = new Rect(id, SUBSTANCE_SIZE, SUBSTANCE_SIZE);
        icon.setFillColor(SUBSTANCE_COLOR);
        addIcon(icon, lat + JIGGLE, lon + JIGGLE);
    }

    private void addIcon(SvgElement icon, int lat, int lon) {
        icon.move(lat * UNIT, lon * UNIT);
        this.atlas.add(icon);
        this.icons.put(icon.getId(), icon);
    }

    public void removeOrganism(Location ignored, Organism organism) {
        removeIcon(String.valueOf(organism.getId()));
    }

    public void removeSubstance(Location location, Substance substance) {
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

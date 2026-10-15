package io.eiaun.shared.physics;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.organisms.Response;
import io.eiaun.shared.util.TwoD;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class ChangeApplier {

    private final TwoD<Organism> organisms;
    private final TwoD<Substance> substances;
    private final int grid;
    private final Consumer<String> rejectedChangeRecorder;


    public ChangeApplier(
            TwoD<Organism> organisms,
            TwoD<Substance> substances,
            int grid,
            Consumer<String> rejectedChangeRecorder
    ) {
        this.organisms = organisms;
        this.substances = substances;
        this.grid = grid;
        this.rejectedChangeRecorder = rejectedChangeRecorder;
    }

    public void changeSubstances(List<Change<Substance>> substanceChanges) {
        changeSubstances(Location.ORIGIN, substanceChanges);
    }

    public void changeSubstances(Location location, List<Change<Substance>> substanceChanges) {
        changeThings("substance",
                Substance::getId,
                location,
                substanceChanges,
                this.substances,
                true);
    }

    public boolean changeOrganisms(List<Change<Organism>> organismChanges) {
        return changeOrganisms(Location.ORIGIN, organismChanges);
    }

    public boolean changeOrganisms(Location location, List<Change<Organism>> organismChanges) {
        return changeThings("organism",
                Organism::getId,
                location,
                organismChanges,
                this.organisms,
                false);
    }

    private <Thing> boolean changeThings(
            String label,
            Function<Thing, Object> describer,
            Location location,
            List<Change<Thing>> changes,
            TwoD<Thing> things,
            // a substance can be changed to a different substance,
            // but an organism cannot be changed to a different organism
            boolean mutable
    ) {
        boolean modified = false;
        for (var change : changes) {
            boolean rejected = false;
            if (change.isCreate()) {
                Location to = location.add(change.getTo(), this.grid);
                int lat = to.getLat();
                int lon = to.getLon();
                if (things.contains(lat, lon)) {
                    // another thing is already at the destination
                    rejected = true;
                } else {
                    things.set(lat, lon, change.getReplacement());
                    modified = true;
                }
            } else if (change.isDestroy()) {
                Location from = location.add(change.getFrom(), this.grid);
                int lat = from.getLat();
                int lon = from.getLon();
                if (!Objects.equals(things.get(lat, lon), change.getOriginal())) {
                    // already changed, or no-op (already deleted)
                    rejected = true;
                } else {
                    things.remove(lat, lon);
                    modified = true;
                }
            } else if (change.isReplace()) {
                Location from = location.add(change.getFrom(), this.grid);
                int lat = from.getLat();
                int lon = from.getLon();
                Thing current = things.get(lat, lon);
                Thing original = change.getOriginal();
                Thing replacement = change.getReplacement();
                if (!mutable || !Objects.equals(current, original) || Objects.equals(current, replacement)) {
                    // replace immutable thing,
                    // or original is already gone,
                    // or no-op (already replaced)
                    rejected = true;
                } else {
                    things.set(lat, lon, replacement);
                    modified = true;
                }
            } else if (change.isMove()) {
                Location from = location.add(change.getFrom(), this.grid);
                int flat = from.getLat();
                int flon = from.getLon();
                Location to = location.add(change.getTo(), this.grid);
                int tlat = to.getLat();
                int tlon = to.getLon();
                if (!Objects.equals(things.get(flat, flon), change.getOriginal()) ||
                        things.contains(tlat, tlon)) {
                    // original object changed or been removed,
                    // or another thing is already at the destination
                    rejected = true;
                } else {
                    things.remove(flat, flon);
                    things.set(tlat, tlon, change.getOriginal());
                    modified = true;
                }
            } else {
                log.warn(String.format("Gibberish change: %s", change));
                rejected = true;
            }
            if (rejected) {
                this.rejectedChangeRecorder.accept(String.format(
                        "Rejecting %s change: %s -> %s; %s -> %s",
                        label,
                        Optional.ofNullable(change.getOriginal()).map(describer).orElse(null),
                        Optional.ofNullable(change.getReplacement()).map(describer).orElse(null),
                        change.getFrom(),
                        change.getTo()));
            }
        }
        return modified;
    }

}
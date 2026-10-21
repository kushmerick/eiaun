package io.eiaun.shared.physics;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.util.Functionals;
import io.eiaun.shared.util.TwoD;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class ChangeApplier {

    private final int grid;
    private final Consumer<String> rejectedChangeRecorder;
    private final SubstanceFactory substanceFactory;

    public ChangeApplier(
            int grid,
            Consumer<String> rejectedChangeRecorder,
            SubstanceFactory substanceFactory
    ) {
        this.grid = grid;
        this.rejectedChangeRecorder = rejectedChangeRecorder;
        this.substanceFactory = substanceFactory;
    }

    public void changeSubstances(
            TwoD<Substance> substances,
            Location location,
            List<Change<String>> substanceChanges
    ) {
        changeThings("substance",
                location,
                substanceChanges,
                substances,
                true,
                this.substanceFactory::make,
                Functionals.noop(),
                Functionals.noop());
    }

    public boolean changeOrganisms(
            TwoD<Organism> organisms,
            Location location,
            List<Change<Long>> organismChanges,
            Map<Long, Organism> index
    ) {
        return changeThings("organism",
                location,
                organismChanges,
                organisms,
                false,
                index::get,
                organism -> index.put(organism.getId(), organism),
                organism -> index.remove(organism.getId()));
    }

    private <Thing, Id> boolean changeThings(
            String label,
            Location location,
            List<Change<Id>> changes,
            TwoD<Thing> things,
            // a substance can be changed to a different substance,
            // but an organism cannot be changed to a different organism
            boolean mutable,
            // three methods related to mapping Id to Thing (and optionally updating a backing index if desired)
            Function<Id, Thing> resolver,
            Consumer<Thing> setCallback,
            Consumer<Thing> removeCallback
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
                    Thing newThing = resolver.apply(change.getReplacement());
                    things.set(lat, lon, newThing);
                    setCallback.accept(newThing);
                    modified = true;
                }
            } else if (change.isDestroy()) {
                Location from = location.add(change.getFrom(), this.grid);
                int lat = from.getLat();
                int lon = from.getLon();
                Thing oldThing = resolver.apply(change.getOriginal());
                if (!Objects.equals(things.get(lat, lon), oldThing)) {
                    // already changed, or no-op (already deleted)
                    rejected = true;
                } else {
                    things.remove(lat, lon);
                    removeCallback.accept(oldThing);
                    modified = true;
                }
            } else if (change.isReplace()) {
                Location from = location.add(change.getFrom(), this.grid);
                int lat = from.getLat();
                int lon = from.getLon();
                Thing current = things.get(lat, lon);
                Thing original = resolver.apply(change.getOriginal());
                Thing replacement = resolver.apply(change.getReplacement());
                if (!mutable || !Objects.equals(current, original) || Objects.equals(current, replacement)) {
                    // replace immutable thing,
                    // or original is already gone,
                    // or no-op (already replaced)
                    rejected = true;
                } else {
                    things.set(lat, lon, replacement);
                    removeCallback.accept(original);
                    setCallback.accept(replacement);
                    modified = true;
                }
            } else if (change.isMove()) {
                Location from = location.add(change.getFrom(), this.grid);
                int flat = from.getLat();
                int flon = from.getLon();
                Location to = location.add(change.getTo(), this.grid);
                int tlat = to.getLat();
                int tlon = to.getLon();
                Thing thing = resolver.apply(change.getOriginal());
                if (!Objects.equals(things.get(flat, flon), thing) ||
                        things.contains(tlat, tlon)) {
                    // original object changed or been removed,
                    // or another thing is already at the destination
                    rejected = true;
                } else {
                    things.remove(flat, flon);
                    things.set(tlat, tlon, thing);
                    // just a move so no need to invoke setCallback or removeCallback
                    modified = true;
                }
            } else {
                log.warn(String.format("Gibberish change: %s", change));
                rejected = true;
            }
            if (rejected) {
                // a change is rejected if it is not applicable to the actual state of the substances and organisms.
                // this happens during the simulation because organisms run in parallel.  so once in a while,
                // two nearby organisms may interact in a way that makes of their actions incompatible (eg, eating
                // the same substance). the "winner" is whichever happens to upgate the state first.
                // TODO: During a simulation, it seems reasonable to ignore rejected changes.
                // TODO: What else could we do?  But during snapshot play-back, are we sure
                // TODO: we're actually tracking the changes correctly?  As a solution,
                // TODO: perhaps in case of any rejected change, we should take a full dump?
                this.rejectedChangeRecorder.accept(String.format(
                        "Rejecting %s change: %s -> %s; %s -> %s",
                        label,
                        change.getOriginal(),
                        change.getReplacement(),
                        change.getFrom(),
                        change.getTo()));
            }
        }
        return modified;
    }

}
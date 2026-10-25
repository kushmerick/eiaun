package io.eiaun.shared.physics;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.organisms.State;
import io.eiaun.shared.util.Generic;
import io.eiaun.shared.util.TwoD;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class ChangeApplier {

    private final int grid;
    private final Consumer<String> rejectedChangeRecorder;

    public ChangeApplier(
            int grid,
            Consumer<String> rejectedChangeRecorder
    ) {
        this.grid = grid;
        this.rejectedChangeRecorder = rejectedChangeRecorder;
    }

    public void changeSubstances(
            TwoD<Substance> substances,
            Location location,
            List<Change<String,Substance>> changes,
            Function<String,Substance> resolver,
            BiConsumer<Location, Substance>  createdCallback,
            BiConsumer<Location, Substance>  destroyedCallback
    ) {
        changeThings("substance",
                location,
                changes,
                substances,
                resolver,
                createdCallback,
                destroyedCallback,
                null);
    }

    public boolean changeOrganisms(
            TwoD<Organism> organisms,
            Location location,
            List<Change<Long,Organism>> changes,
            Function<Long,Organism> resolver,
            BiConsumer<Location, Organism>  createdCallback,
            BiConsumer<Location, Organism>  destroyedCallback
    ) {
        return changeThings("organism",
                location,
                changes,
                organisms,
                resolver,
                createdCallback,
                destroyedCallback,
                (organism, state) -> organism.setState(state.decode(State.KIND_STATES)));
    }

    private <Thing, Id> boolean changeThings(
            String label,
            Location location,
            List<Change<Id,Thing>> changes,
            TwoD<Thing> things,
            // three methods related to mapping Id to Thing (and updating the backing index for organisms)
            Function<Id, Thing> resolver,
            BiConsumer<Location, Thing> createdCallback,
            BiConsumer<Location, Thing> destroyedCallback,
            // Just for Organisms: state updating function
            BiConsumer<Thing, Generic> stateUpdater
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
                    Thing baby = change.getBaby();
                    things.set(lat, lon, baby);
                    createdCallback.accept(to, baby);
                    modified = true;
                }
            } else if (change.isDestroy()) {
                Location from = location.add(change.getFrom(), this.grid);
                int lat = from.getLat();
                int lon = from.getLon();
                Thing oldThing = resolver.apply(change.getOriginal());
                if (!Objects.equals(things.get(lat, lon), oldThing) || oldThing == null) {
                    // already changed, or no-op (already deleted)
                    rejected = true;
                } else {
                    things.remove(lat, lon);
                    destroyedCallback.accept(from, oldThing);
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
                    destroyedCallback.accept(from, thing);
                    things.set(tlat, tlon, thing);
                    createdCallback.accept(to, thing);
                    modified = true;
                }
            } else if (change.isUpdate()) {
                Location to = location.add(change.getTo(), this.grid);
                int lat = to.getLat();
                int lon = to.getLon();
                Thing thing = things.get(lat, lon);
                if (thing == null || stateUpdater == null) {
                    // updated thing does not exist, or does not have updatable state
                    rejected = true;
                } else {
                    stateUpdater.accept(thing, change.getState());
                }
            } else {
                log.warn("Gibberish change: {}", change);
                rejected = true;
            }
            if (rejected) {
                // A change is rejected if it is not applicable to the actual state of the substances and organisms.
                // This happens during the simulation because organisms run in parallel. So it can happen that
                // two nearby organisms may interact in a way that makes of their actions incompatible (eg, eating
                // the same substance). The "winner" is whichever happens to upgate the state first.  An important
                // question is whether rejection impacts replaying changes during snapshot processing by (for example)
                // VIZ.  Since we take only occasional full dumps, is it possible that rejecting changes will
                // screw up correctly tracking the organisms and substances? It seems the answer should be: No.
                // Whether a change is rejected or not, the resulting state of the world during change replay
                // should be identical to what it would have been during the actual simulation.  As long as we
                // replay all changes in the same order, and the snapshot index mechanism guarantees that we do so.
                this.rejectedChangeRecorder.accept(String.format(
                        "Rejecting %s %s change: %s -> %s; %s -> %s; %s; %s",
                        change.getVerb(),
                        label,
                        change.getOriginal(),
                        change.getReplacement(),
                        change.getFrom(),
                        change.getTo(),
                        change.getBaby(),
                        change.getState()));
            }
        }
        return modified;
    }

}
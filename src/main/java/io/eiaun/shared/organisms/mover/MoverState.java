package io.eiaun.shared.organisms.mover;

import io.eiaun.shared.organisms.State;
import lombok.Getter;
import org.apache.fory.json.annotation.JsonProperty;

public class MoverState extends State {

    @JsonProperty("e") @Getter public double energy;

    public MoverState(double energy) {
        this.energy = energy;
    }

    @Override public boolean isSame(State state) {
        return state instanceof MoverState ms && this.energy == ms.energy;
    }

}

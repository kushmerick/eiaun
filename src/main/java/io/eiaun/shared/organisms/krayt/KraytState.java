package io.eiaun.shared.organisms.krayt;

import io.eiaun.shared.organisms.State;
import lombok.Getter;
import org.apache.fory.json.annotation.JsonProperty;

public class KraytState extends State {

    @JsonProperty("e") @Getter public double energy;

    public KraytState(double energy) {
        this.energy = energy;
    }

    @Override public boolean isSame(State state) {
        return state instanceof KraytState ms && this.energy == ms.energy;
    }

}

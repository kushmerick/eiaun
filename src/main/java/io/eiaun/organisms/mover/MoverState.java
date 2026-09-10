package io.eiaun.organisms.mover;

import io.eiaun.organisms.State;
import lombok.Getter;

public class MoverState implements State {

    @Getter public final double energy;

    public MoverState(double energy) {
        this.energy = energy;
    }
}

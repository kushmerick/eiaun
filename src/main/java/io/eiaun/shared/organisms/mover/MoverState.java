package io.eiaun.shared.organisms.mover;

import io.eiaun.shared.organisms.State;
import lombok.Getter;

public class MoverState extends State {

    @Getter public final double energy;

    public MoverState(double energy) {
        this.energy = energy;
    }

}

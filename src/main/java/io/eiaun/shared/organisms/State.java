package io.eiaun.shared.organisms;

import io.eiaun.shared.organisms.mover.MoverState;
import io.eiaun.shared.organisms.simple.SimpleState;

import java.util.Map;
import java.util.stream.Collectors;

public abstract class State {

    public static final Map<String, Class<?>> KIND_STATES = Map.of(
            "M", MoverState.class,
            "S", SimpleState.class
    );

    public static final Map<Class<?>, String> STATE_KINDS =
            KIND_STATES.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    public boolean equals(Object thing) {
        return thing instanceof State state && this.isSame(state);
    }

    abstract public boolean isSame(State that);

}

package io.eiaun.shared.organisms;

public abstract class State {

    public boolean equals(Object obj) {
        return obj instanceof State state && this.equals(state);
    }

    abstract public boolean equals(State that);

}

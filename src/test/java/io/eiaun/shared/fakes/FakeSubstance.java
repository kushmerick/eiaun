package io.eiaun.shared.fakes;

import io.eiaun.shared.physics.Substance;

import java.util.Collections;
import java.util.Objects;

public class FakeSubstance extends Substance {

    private static int ID = 0;

    public FakeSubstance() {
        this("fake" + ID++);
    }

    public FakeSubstance(String id) {
        super(id, Collections.emptyMap(), null);
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof FakeSubstance thats) {
            return getId().equals(thats.getId());
        } else {
            return false;
        }
    }

}

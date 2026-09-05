package io.eiaun.fakes;

import io.eiaun.physics.Substance;

import java.util.Collections;

public class FakeSubstance extends Substance {

    private static int ID = 0;

    public FakeSubstance() {
        super("fake" + ID++, Collections.emptyMap());
    }

}

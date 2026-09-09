package io.eiaun.fakes;

import io.eiaun.physics.Substance;
import io.eiaun.physics.SubstanceFactory;

import java.util.Collections;

public class FakeSubstanceFactory extends SubstanceFactory {

    public FakeSubstanceFactory() {
        super(Collections.emptyList());
    }

    @Override
    public Substance make() {
        return new FakeSubstance();
    }

}

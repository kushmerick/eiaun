package io.eiaun.shared.fakes;

import io.eiaun.shared.physics.Substance;
import io.eiaun.shared.physics.SubstanceFactory;

import java.util.Collections;

public class FakeSubstanceFactory extends SubstanceFactory {

    public FakeSubstanceFactory() {
        super(Collections.emptyList());
    }

    @Override
    public Substance make() {
        return new FakeSubstance();
    }

    @Override
    public Substance make(String id) {
        return new FakeSubstance(id);
    }

}

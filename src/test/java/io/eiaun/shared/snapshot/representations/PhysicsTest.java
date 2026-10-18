package io.eiaun.shared.snapshot.representations;

import io.eiaun.shared.physics.Jakku;
import io.eiaun.shared.physics.SubstanceFactory;
import io.eiaun.shared.physics.SubstanceSpec;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PhysicsTest {

    @Test
    void canFrom() {
        Map<String, SubstanceSpec> substanceSpecs = Collections.emptyMap();
        SubstanceFactory substanceFactory = mock(SubstanceFactory.class);
        when(substanceFactory.getSubstanceSpecs()).thenReturn(substanceSpecs);
        Jakku jakku = mock(Jakku.class);
        when(jakku.getSubstanceFactory()).thenReturn(substanceFactory);
        Physics physics = Physics.from(jakku);
        assertSame(jakku.getSubstanceFactory().getSubstanceSpecs(), physics.getSubstanceSpecs());
    }

}
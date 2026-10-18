package io.eiaun.shared.snapshot.representations;

import io.eiaun.shared.physics.Jakku;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigTest {

    @Test
    void canFrom() {
        Jakku jakku = mock(Jakku.class);
        when(jakku.getGrid()).thenReturn(1234);
        when(jakku.getOrganismDensity()).thenReturn(12.34);
        when(jakku.getSubstanceDensity()).thenReturn(56.78);
        Config config = Config.from(jakku);
        assertEquals(jakku.getGrid(), config.getGrid());
        assertEquals(jakku.getOrganismDensity(), config.getOrganismDensity());
        assertEquals(jakku.getSubstanceDensity(), config.getSubstanceDensity());
    }

}
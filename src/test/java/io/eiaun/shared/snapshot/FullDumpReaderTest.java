package io.eiaun.shared.snapshot;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.physics.Substance;
import io.eiaun.shared.physics.SubstanceFactory;
import io.eiaun.shared.util.TwoD;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

import static org.apache.commons.compress.compressors.CompressorStreamFactory.ZSTANDARD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullDumpReaderTest {

    @Test
    void canLoadOrganisms() {
        FullDumpReader fullDumpReader = new FullDumpReader(
                ZSTANDARD,
                new SubstanceFactory(Collections.emptyList()));
        Path snapshot = Paths.get("src/test/resources/test-data/0000000000000-2026-10-12-07-16-02-78");
        Optional<TwoD<Organism>> organisms = fullDumpReader.maybeGetOrganisms(snapshot);
        assertTrue(organisms.isPresent());
        assertEquals(372050, organisms.get().get(0, 907).getId());
    }

    @Test
    void canLoadSubstances() {
        FullDumpReader fullDumpReader = new FullDumpReader(
                ZSTANDARD,
                new SubstanceFactory(Collections.emptyList()) {
                    @Override
                    public Substance make(String id) {
                        return new Substance(id, null, null);
                    }
                });
        Path snapshot = Paths.get("src/test/resources/test-data/0000000000000-2026-10-12-07-16-02-78");
        Optional<TwoD<Substance>> substances = fullDumpReader.maybeGetSubstances(snapshot);
        assertTrue(substances.isPresent());
        assertEquals("Bohrium", substances.get().get(0, 907).getId());
    }

}
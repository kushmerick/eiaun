package io.eiaun.shared.snapshot;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.physics.Substance;
import io.eiaun.shared.physics.SubstanceFactory;
import io.eiaun.shared.util.TwoD;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

import static org.apache.commons.compress.compressors.CompressorStreamFactory.ZSTANDARD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FullDumpReaderTest {

    public static final Path RECORDINGS = Path.of("src/test/resources/test-recordings");
    public static final Path RECORDING = RECORDINGS.resolve("2026-11-01-15/2026-11-01-15-12/2026-11-01-15-12-31-275");

    @Mock
    private SubstanceFactory substanceFactory;

    @Test
    void canLoadOrganisms() {
        FullDumpReader fullDumpReader = new FullDumpReader(
                ZSTANDARD,
                this.substanceFactory);
        Optional<TwoD<Organism>> organisms = fullDumpReader.maybeGetOrganisms(RECORDING);
        assertTrue(organisms.isPresent());
        assertEquals(60687, organisms.get().get(0, 350).getId());
    }

    @Test
    void canLoadSubstances() {
        when(this.substanceFactory.make(anyString())).then(this::makeThatSubstance);
        FullDumpReader fullDumpReader = new FullDumpReader(
                ZSTANDARD,
                this.substanceFactory);
        Optional<TwoD<Substance>> substances = fullDumpReader.maybeGetSubstances(RECORDING);
        assertTrue(substances.isPresent());
        assertEquals("Berkelium", substances.get().get(0, 48).getId());
    }

    private Substance makeThatSubstance(InvocationOnMock invocation) {
        return new Substance(invocation.getArgument(0), null, null);
    }

}
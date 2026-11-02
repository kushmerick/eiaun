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
import java.util.Optional;

import static io.eiaun.shared.testresources.TestRecordings.RECORDING;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.ZSTANDARD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FullDumpReaderTest {

    @Mock
    private SubstanceFactory substanceFactory;

    @Test
    void canLoadOrganisms() {
        // $ zstdcat src/test/resources/test-recordings/*/*/*/organisms.json.zstd | jq . | head -5
        // {
        //  "0": {
        //    "350": {
        //      "k": "M",
        //      "j": "{\"id\":49383,\"s\":{\"e\":5.0},\"g\":{\"d\":{\"OxidizingAgent\":\"VFk1W\",\"Adhesion\":\"VKbA4\"},\"v\":3.0,\"p\":5.0,\"m\":1.0,\"r\":0.5}}"
        FullDumpReader fullDumpReader = new FullDumpReader(
                ZSTANDARD,
                this.substanceFactory);
        Optional<TwoD<Organism>> organisms = fullDumpReader.maybeGetOrganisms(RECORDING);
        assertTrue(organisms.isPresent());
        assertEquals(49383, organisms.get().get(0, 350).getId());
    }

    @Test
    void canLoadSubstances() {
        // $ zstdcat src/test/resources/test-recordings/*/*/*/substances.json.zstd | jq . | head -3
        // {
        //  "0": {
        //    "88": "Hassium",
        when(this.substanceFactory.make(anyString())).then(this::makeThatSubstance);
        FullDumpReader fullDumpReader = new FullDumpReader(
                ZSTANDARD,
                this.substanceFactory);
        Optional<TwoD<Substance>> substances = fullDumpReader.maybeGetSubstances(RECORDING);
        assertTrue(substances.isPresent());
        assertEquals("Hassium", substances.get().get(0, 88).getId());
    }

    private Substance makeThatSubstance(InvocationOnMock invocation) {
        return new Substance(invocation.getArgument(0), null, null);
    }

}
package io.eiaun.shared.snapshot;

import io.eiaun.shared.organisms.Organism;
import io.eiaun.shared.physics.Substance;
import io.eiaun.shared.physics.SubstanceFactory;
import io.eiaun.shared.util.Functionals;
import io.eiaun.shared.util.Generic;
import io.eiaun.shared.util.JSON;
import io.eiaun.shared.util.TwoD;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.fory.reflect.TypeRef;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class FullDumpReader {

    private final String compression;
    private final SubstanceFactory substanceFactory;

    public FullDumpReader(
            String compression,
            SubstanceFactory substanceFactory
    ) {
        this.compression = compression;
        this.substanceFactory = substanceFactory;
    }

    public Optional<TwoD<Organism>> maybeGetOrganisms(Path snapshot) {
        return maybeGetThings(
                snapshot.resolve(FileSnapshotReader.ORGANISMS_DOT + this.compression),
                (Generic generic) -> generic.decode(Organism.KIND_ORGANISMS),
                new TypeRef<Map<String, Map<String, Generic>>>() {
                }
        );
    }

    public Optional<TwoD<Substance>> maybeGetSubstances(Path snapshot) {
        return maybeGetThings(
                snapshot.resolve(FileSnapshotReader.SUBSTANCES_DOT + this.compression),
                this.substanceFactory::make,
                new TypeRef<Map<String, Map<String, String>>>() {
                });
    }

    private <Thing, Representation> Optional<TwoD<Thing>> maybeGetThings(
            Path thingsPath,
            Function<Representation, Thing> transformer,
            TypeRef<Map<String,Map<String,Representation>>> typeRef
    ) {
        if (Files.exists(thingsPath)) {
            Map<String, Map<String, Representation>> map = Functionals.get(() -> JSON.fromJson(
                    readCompressed(thingsPath),
                    typeRef));
            TwoD<Thing> twod = new TwoD<>();
            for (var outer : map.entrySet()) {
                int lat = Integer.parseInt(outer.getKey());
                for (var inner : outer.getValue().entrySet()) {
                    int lon = Integer.parseInt(inner.getKey());
                    twod.set(lat, lon, transformer.apply(inner.getValue()));
                }
            }
            return Optional.of(twod);
        } else {
            return Optional.empty();
        }
    }

    String readCompressed(Path path) {
        try {
            String format = PathUtils.getExtension(path);
            try (var compressed = Files.newInputStream(path);
                 var buffered = new BufferedInputStream(compressed);
                 var uncompressed = new CompressorStreamFactory().createCompressorInputStream(format, buffered)
            ) {
                return IOUtils.toString(uncompressed, StandardCharsets.UTF_8);
            }
        } catch (IOException failure) {
            throw new RuntimeException("Failure while reading compressed payload", failure);
        }
    }
}
package io.eiaun.physics;

import lombok.Getter;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.math3.distribution.ZipfDistribution;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SubstanceFactory {

    private final Random random = new Random();

    private final Map<String, Substance> singletons = new HashMap<>();

    @Getter
    private final Map<String, SubstanceSpec> substanceSpecs;

    public SubstanceFactory(List<SubstanceSpec> substanceSpecs) {
        this.substanceSpecs = substanceSpecs.stream()
                .collect(Collectors.toMap(SubstanceSpec::getId, Function.identity()));
        double sum = 0;
        for (SubstanceSpec spec: this.substanceSpecs.values()) {
            sum += spec.abundance;
        }
        for (SubstanceSpec spec: this.substanceSpecs.values()) {
            spec.abundance /= sum;
        }
    }

    public Substance make() {
        double threshold = this.random.nextDouble();
        double cumulative = 0;
        for (SubstanceSpec spec: this.substanceSpecs.values()) {
            cumulative += spec.abundance;
            if (threshold < cumulative) {
                return singletonMake(spec);
            }
        }
        throw new RuntimeException("Invalid atom abundance");
    }

    public Substance make(String id) {
        return singletonMake(this.substanceSpecs.get(id));
    }

    private Substance singletonMake(SubstanceSpec spec) {
        // the singleton cache must be synchronized, because during the simulation
        // organisms may be creating substances concurrently
        synchronized (this) {
            return this.singletons.computeIfAbsent(spec.getId(), _ -> spec.make());
        }
    }

    // generate random atom specifications.  types follow a zipfian distribution

    private static class SkipNullRepresenter extends Representer {

        public SkipNullRepresenter(DumperOptions options) {
            super(options);
        }

        @Override
        protected NodeTuple representJavaBeanProperty(
                Object javaBean,
                Property property,
                Object propertyValue,
                Tag customTag
        ) {
            if (propertyValue == null) {
                // omit null properties
                return null;
            }
            return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
        }

    }

    public static void main(String[] args) {
        int nTypes = 30;
        int nProperties = nTypes / 3; // lots more types than properties to encourage collisions
        int nValues = nTypes / 10;    // way more types than values to really encourage collisions
        int labelLen = 4;             // long enough so collisions should never happen
        double childProb = 0.5;       // half the substances don't decay; half decay to another random substance
        Random random = new Random();
        RandomStringUtils randomStringUtils = RandomStringUtils.secure();
        double abundanceZipfExponent = 1;
        ZipfDistribution abundanceZipf = new ZipfDistribution(nTypes, abundanceZipfExponent);
        String valuePrefix = "V";
        Map<String, List<String>> propertyValues = new HashMap<>();
        for (int i = 0; i < nProperties; i++) {
            String property = Stream
                    .generate(() -> ATOM_PROPERTIES[random.nextInt(ATOM_PROPERTIES.length)])
                    .filter(Predicate.not(propertyValues::containsKey)) // prevent duplicates
                    .findFirst()
                    .orElseThrow();
            List<String> values = new ArrayList<>();
            for (int j = 0; j < nValues; j++) {
                values.add(valuePrefix + randomStringUtils.nextAlphanumeric(labelLen));
            }
            propertyValues.put(property, values);
        }
        List<SubstanceSpec> specs = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for  (int i = 0; i < nTypes; i++) {
            String id = Stream
                    .generate(() -> ATOM_TYPE_IDS[random.nextInt(ATOM_TYPE_IDS.length)])
                    .filter(id2 -> !ids.contains(id2)) // prevent duplicates
                    .findFirst()
                    .orElseThrow();
            ids.add(id);
        }
        for (String id: ids) {
            Map<String,String> properties = new HashMap<>();
            for (var entry: propertyValues.entrySet()) {
                properties.put(
                        entry.getKey(),
                        entry.getValue().get(random.nextInt(nValues)));
            }
            String child = id;
            while (Objects.equals(child, id)) {
                child = random.nextDouble() < childProb
                        ? ids.toArray(new String[0])[random.nextInt(ids.size())]
                        : null;
            }
            specs.add(new SubstanceSpec(id, properties, child, abundanceZipf.sample() /* ensure positive */ + 1));
        }
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Representer representer = new SkipNullRepresenter(dumperOptions);
        representer.addClassTag(SubstanceSpec.class, Tag.MAP);
        Yaml yaml = new Yaml(representer, dumperOptions);
        System.out.println(yaml.dump(specs));
    }

    private static final String[] ATOM_TYPE_IDS = {
            "Hydrogen",
            "Helium",
            "Lithium",
            "Beryllium",
            "Boron",
            "Carbon",
            "Nitrogen",
            "Oxygen",
            "Fluorine",
            "Neon",
            "Sodium",
            "Magnesium",
            "Aluminum",
            "Silicon",
            "Phosphorus",
            "Sulfur",
            "Chlorine",
            "Argon",
            "Potassium",
            "Calcium",
            "Scandium",
            "Titanium",
            "Vanadium",
            "Chromium",
            "Manganese",
            "Iron",
            "Cobalt",
            "Nickel",
            "Copper",
            "Zinc",
            "Gallium",
            "Germanium",
            "Arsenic",
            "Selenium",
            "Bromine",
            "Krypton",
            "Rubidium",
            "Strontium",
            "Yttrium",
            "Zirconium",
            "Niobium",
            "Molybdenum",
            "Technetium",
            "Ruthenium",
            "Rhodium",
            "Palladium",
            "Silver",
            "Cadmium",
            "Indium",
            "Tin",
            "Antimony",
            "Tellurium",
            "Iodine",
            "Xenon",
            "Cesium",
            "Barium",
            "Lanthanum",
            "Cerium",
            "Praseodymium",
            "Neodymium",
            "Promethium",
            "Samarium",
            "Europium",
            "Gadolinium",
            "Terbium",
            "Dysprosium",
            "Holmium",
            "Erbium",
            "Thulium",
            "Ytterbium",
            "Lutetium",
            "Hafnium",
            "Tantalum",
            "Tungsten",
            "Rhenium",
            "Osmium",
            "Iridium",
            "Platinum",
            "Gold",
            "Mercury",
            "Thallium",
            "Lead",
            "Bismuth",
            "Polonium",
            "Astatine",
            "Radon",
            "Francium",
            "Radium",
            "Actinium",
            "Thorium",
            "Protactinium",
            "Uranium",
            "Neptunium",
            "Plutonium",
            "Americium",
            "Curium",
            "Berkelium",
            "Californium",
            "Einsteinium",
            "Fermium",
            "Mendelevium",
            "Nobelium",
            "Lawrencium",
            "Rutherfordium",
            "Dubnium",
            "Seaborgium",
            "Bohrium",
            "Hassium",
            "Meitnerium",
            "Darmstadtium",
            "Roentgenium",
            "Copernicium",
            "Nihonium",
            "Flerovium",
            "Moscovium",
            "Livermorium",
            "Tennessine",
            "Oganesson",
    };

    private static final String[] ATOM_PROPERTIES = {
            "AbsoluteMolarMass",
            "AcidityFunction",
            "Adhesion",
            "AirSensitivity",
            "Amphiphile",
            "Amphoterism",
            "Anhydrous",
            "AnilinePoint",
            "Apicophilicity",
            "AqueousSolution",
            "AtomicMass",
            "AtomicNumber",
            "RelativeAtomicMass",
            "AutoignitionTemperature",
            "BoilingPointElevation",
            "ChaotropicAgent",
            "ChemicalComposition",
            "ChemicalPolarity",
            "ChemicallyInert",
            "CloudPoint",
            "ColdFilterPluggingPoint",
            "CombustibilityAndFlammability",
            "Corrosivity",
            "CovalentRadius",
            "CriticalRelativeHumidity",
            "CrystalStructure",
            "DefiningEquation",
            "Dopant",
            "ElectricCharge",
            "ElectronAffinity",
            "ElectronAffinity",
            "ElectronConfiguration",
            "Electronegativity",
            "EnergyLevel",
            "EsterValue",
            "FieldEffect",
            "FirePoint",
            "FreezingPointDepression",
            "Fusibility",
            "GalvanicSeries",
            "GelPoint",
            "HydrationNumber",
            "Hydrophile",
            "Hydrophobe",
            "Hygroscopy",
            "IonicPotential",
            "IonizationEnergy",
            "Kosmotropic",
            "KrögerVinkNotation",
            "Lipophobicity",
            "LNAPLTransmissivity",
            "LowerFlammabilityLimit",
            "LyotropicLiquidCrystal",
            "MassNumber",
            "MassFluxFraction",
            "Metastability",
            "Miscibility",
            "MixingRatio",
            "Molality",
            "MolarMass",
            "MolecularMass",
            "MonoisotopicMass",
            "OxidationState",
            "OxidizingAgent",
            "Polyvalency",
            "PourPoint",
            "ProtonAffinity",
            "Pyrophoricity",
            "Reactivity",
            "Refractory",
            "ReidVaporPressure",
            "SmokePoint",
            "Solubility",
            "SolvationShell",
            "SpecificRotation",
            "SpeedOfSound",
            "Superhydrophilicity",
            "Tactoid",
            "Tetravalence",
            "Transferability",
            "TriplePoint",
            "TrueVaporPressure",
            "Ultrahydrophobicity",
            "Valence",
            "VanDerWaalsRadius",
            "Vapor",
            "Volatility",
    };

}

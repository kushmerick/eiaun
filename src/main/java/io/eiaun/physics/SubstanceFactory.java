package io.eiaun.physics;

import lombok.Getter;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.math3.distribution.ZipfDistribution;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SubstanceFactory {

    private final Random random = new Random();

    @Getter
    private final List<SubstanceSpec> substanceSpecs;

    public SubstanceFactory(List<SubstanceSpec> substanceSpecs) {
        this.substanceSpecs = substanceSpecs;
        double sum = 0;
        for (SubstanceSpec spec: this.substanceSpecs) {
            sum += spec.abundance;
        }
        for (SubstanceSpec spec: this.substanceSpecs) {
            spec.abundance /= sum;
        }
    }

    public Substance make() {
        double threshold = this.random.nextDouble();
        double cumulative = 0;
        for (SubstanceSpec spec: this.substanceSpecs) {
            cumulative += spec.abundance;
            if (threshold < cumulative) {
                return spec.make();
            }
        }
        throw new RuntimeException("Invalid atom abundance");
    }

    // generate random atom specifications.  types follow a zipfian distribution;
    public static void main(String[] args) {
        int nTypes = 30;
        int nProperties = nTypes / 3; // lots more types than properties to encourage collisions
        int nValues = nTypes / 10;    // way more types than values to really encourage collisions
        int labelLen = 4;             // long enough so collisions should never happen
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
        for  (int i = 0; i < nTypes; i++) {
            String id = Stream
                    .generate(() -> ATOM_TYPE_IDS[random.nextInt(ATOM_TYPE_IDS.length)])
                    .filter(id2 -> specs.stream().noneMatch(s -> s.id.equals(id2))) // prevent duplicates
                    .findFirst()
                    .orElseThrow();
            Map<String,String> properties = new HashMap<>();
            for (var entry: propertyValues.entrySet()) {
                properties.put(
                        entry.getKey(),
                        entry.getValue().get(random.nextInt(nValues)));
            }
            specs.add(new SubstanceSpec(id, abundanceZipf.sample() /* ensure positive */ + 1, properties));
        }
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Representer representer = new Representer(dumperOptions);
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
            "Boiling-pointElevation",
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
            "Freezing-pointDepression",
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
            "Kröger–VinkNotation",
            "Lipophobicity",
            "LNAPLTransmissivity",
            "LowerFlammabilityLimit",
            "LyotropicLiquidCrystal",
            "MassNumber",
            "Mass-fluxFraction",
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

package be.ugent.idlab.knows;

import be.ugent.idlab.knows.wc2.API;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScenarioTests {

    @BeforeAll
    public static void initLogger() {
        Path logConfigPath = Path.of("log4j2.xml");
        String absLogPath = logConfigPath.toAbsolutePath().toString();
        Configuration config = ConfigurationFactory.getInstance()
                .getConfiguration(
                        null,
                        null,
                        URI.create("file://" + absLogPath));
        Configurator.reconfigure(config);
    }

    static final Path rootPath = Path.of("src", "test", "resources", "scenarios");

    private static Stream<Arguments> scenarioArguments() {
        return Stream.of(
                // Parallel paths; all paths have to be visited (AND)
                Arguments.of("stepsAND", "data_01.ttl"),
                Arguments.of("stepsAND", "data_02.ttl"),
                Arguments.of("stepsAND", "data_03.ttl"),
                Arguments.of("stepsAND", "data_04.ttl"),
                Arguments.of("stepsXOR", "data_01.ttl"),

                // Mutual exclusive paths; exactly one of the paths have to be visited (XOR)
                // option 1
                Arguments.of("stepsXOR", "data_option1_01.ttl"),
                Arguments.of("stepsXOR", "data_option1_02.ttl"),
                // option 2
                Arguments.of("stepsXOR", "data_option2_01.ttl"),
                Arguments.of("stepsXOR", "data_option2_02.ttl"),
                Arguments.of("stepsXOR", "data_option2_03.ttl"),

                // Add some N3 reasoning in the mix, comparing timestamps.
                Arguments.of("stepsWait", "data_01_start.ttl"),
                Arguments.of("stepsWait", "data_02_at_traffic_light.ttl"),
                Arguments.of("stepsWait", "data_03_light_is_green.ttl"),

                // Baking cake tutorial tests
                Arguments.of("bakingCake", "data_1.ttl"),
                Arguments.of("bakingCake", "data_2.ttl"),
                Arguments.of("bakingCake", "data_3.ttl"),
                Arguments.of("bakingCake", "data_4_1_before_wait.ttl"),
                Arguments.of("bakingCake", "data_4_2_after_wait.ttl")

        );
    }

    @ParameterizedTest()
    @MethodSource("scenarioArguments")
    public void runScenario(final String name, final String dataFileName) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        Path inputDir = rootPath.resolve(name);
        String dataFileBaseName = dataFileName.substring(0, dataFileName.length() - 4);

        // Check if there's a "knowledge.n3" file for reasoning
        Path reasoningFilePath = inputDir.resolve("knowledge.n3");
        String reasoningFileName = Files.exists(reasoningFilePath) ? reasoningFilePath.getFileName().toString() : null;

        Path outputDir = inputDir.resolve("output_" + dataFileBaseName);
        API.run(inputDir.toAbsolutePath().toString(),   // input dir
                outputDir.toAbsolutePath().toString(),  // output dir
                null,   // default eye bin, "eye"
                reasoningFileName,   // reasoning file (N3 rules)
                dataFileName);
        Path expectedOutputPath = inputDir.resolve("expected_output_" + dataFileBaseName);

        // compare files in output_* and expected_output_* dirs
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(expectedOutputPath)) {
            // We assume every path points to a file, not a directory
            for (Path expectedOutputFile : stream) {
                String fileName = expectedOutputFile.getFileName().toString();
                Path actualOutputFile = outputDir.resolve(fileName);
                // First check if the file exists in the output directory. It should!
                assertTrue(Files.exists(actualOutputFile), "The file " + fileName + " does not exist in output directory " + outputDir);
                // Then compare the files
                if (fileName.endsWith(".ttl")) {
                    // compare graphs
                    Graph expectedGraph = RDFDataMgr.loadGraph(expectedOutputFile.toString(), Lang.TURTLE);
                    Graph actualGraph = RDFDataMgr.loadGraph(actualOutputFile.toString(), Lang.TURTLE);
                    assertTrue(expectedGraph.isIsomorphicWith(actualGraph), "The file " + fileName + " does not contain the expected graph.");
                } else if (fileName.endsWith(".mmd")) {
                    // compare Mermaid diagrams; exact comparison for now...
                    String expected = Files.readString(expectedOutputFile, StandardCharsets.UTF_8);
                    String actual = Files.readString(actualOutputFile, StandardCharsets.UTF_8);
                    assertEquals(expected, actual, "The generated Mermaid diagram is not exactly the same as the expected one.");
                }
            }
        }
    }
}

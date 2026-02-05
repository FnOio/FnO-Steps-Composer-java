package be.ugent.idlab.knows.wc2;

import be.ugent.idlab.knows.wc2.graph.QueryGraph;
import org.apache.commons.cli.*;
import org.apache.commons.cli.help.HelpFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    static void main(String[] args) throws IOException {
        // CLI parsing...
        Options options = new Options();
        options
                .addRequiredOption("i", "input-dir", true, "Directory containing following input files: shapes.ttl, states.ttl, steps.ttl, goalStates.txt.")
                .addOption("c", "context", true, "Name of a file containing context data (data to operate on)." +
                        "It must exist in the input directory. If not given, all files named `data_*` will be processed alphabetically, as if playing a scenario.")
                .addOption("o", "output-dir", true, "Path to output directory. If not given, the input directory will be used. A subdirectory per context file is created.")
                .addOption("r", "reasoning-file", true, "Name of an N3 file containing extra reasoning on the current context. It must be in the input directory.")
                .addOption("h", "help",  false, "Print this help message.");
        ;

        CommandLineParser parser = new DefaultParser();
        CommandLine cli;
        try {
            cli = parser.parse(options, args);
            if (!cli.hasOption("help")) {

                Path scenarioDir = Path.of(cli.getOptionValue("input-dir")).toAbsolutePath();
                Path shapesPath = scenarioDir.resolve("shapes.ttl");
                Path statesPath = scenarioDir.resolve("states.ttl");
                Path stepsPath = scenarioDir.resolve("steps.ttl");
                Path goalStatesPath = scenarioDir.resolve("goalStates.txt");
                Path outPath = cli.hasOption("output-dir") ? Paths.get(cli.getOptionValue("output-dir")) : scenarioDir.resolve("output");

                logger.debug("Scenario directory: {}", scenarioDir);

                // Get context file(s)
                List<String> contextFileNames = new LinkedList<>();
                if (cli.hasOption("context")) {
                    contextFileNames.add(cli.getOptionValue("context"));
                } else {
                    contextFileNames = Arrays.stream(scenarioDir.toFile().listFiles())
                            .map(File::getName)
                            .filter(name -> name.startsWith("data_"))
                            .sorted()
                            .toList();
                }

                logger.debug("Context files: {}", contextFileNames);

                // Initialize QueryGraph once; use it for each context file.
                QueryGraphBuilder queryGraphBuilder = QueryGraphBuilder.create(stepsPath.toString(), goalStatesPath.toString(), shapesPath.toString(), statesPath.toString());
                QueryGraph queryGraph = queryGraphBuilder.build();

                // Loop over context files
                for (String contextFileName : contextFileNames) {
                    logger.info("Processing {}", contextFileName);
                    Path contextPath = scenarioDir.resolve(contextFileName);

                    // Get output directory and create if it doesn't exist
                    Path contextOutputFile = outPath.resolve("context.ttl");
                    Files.createDirectories(contextOutputFile.getParent());

                    // Apply reasoning to context, if some n3 file is given. Add these to the context
                    String context = Files.readString(contextPath);
                    if (cli.hasOption("reasoning-file")) {
                        Path reasoningFilePath = scenarioDir.resolve(cli.getOptionValue("reasoning-file"));
                        String extraTriples = new ReasonerWrapper().run(contextPath.toString(), reasoningFilePath.toString());
                        if (!extraTriples.isEmpty()) {
                            logger.debug("Extra triples found!");
                            context += extraTriples;
                        }
                    }
                    Files.writeString(contextOutputFile, context, StandardCharsets.UTF_8);

                    // Finally, compose the workflow!
                    queryGraph.process(contextOutputFile.toString());
                }


            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        HelpFormatter formatter = HelpFormatter.builder().setShowSince(false).get();
        formatter.printHelp("Workflow Composer", "Calculate next steps to take given context data.", options, "Good luck.", true);


        //Path scenarioDir = Path.of("/home/geraldh/projects/2024_bocemon/code/use-case-scenario-c-thermal-fno-steps/scenario_manually/");

//        QueryGraphBuilder queryGraphBuilder = QueryGraphBuilder.create(stepsPath.toString(), goalStatesPath.toString(), shapesPath.toString(), statesPath.toString());
//        QueryGraph queryGraph = queryGraphBuilder.build();
//        queryGraph.process(contextPath.toString());
    }
}

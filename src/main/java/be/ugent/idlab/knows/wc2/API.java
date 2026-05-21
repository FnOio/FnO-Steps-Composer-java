package be.ugent.idlab.knows.wc2;

import be.ugent.idlab.knows.wc2.graph.QueryGraph;
import be.ugent.idlab.knows.wc2.out.MermaidPlanRenderer;
import be.ugent.idlab.knows.wc2.out.PPlanRenderer;
import be.ugent.idlab.knows.wc2.out.PlanRenderer;
import be.ugent.idlab.knows.wc2.out.TextPlanRenderer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class API {
    private static final Logger logger = LoggerFactory.getLogger(API.class);

    /**
     * Runs a scenario and generates a workflow plan for each context file.
     * @param inputDir      The path to the (scenario) input directory. This is the place where all necessary data lives.
     *                      It must contain the following files:
     *                      <ul>
     *                      <li>{@code states.ttl}: A description of the possible states that can make a </li>
     *                      <li>{@code shapes.ttl}: SHACL Shapes associated to states, used to match the data and derive to current status of the plan.</li>
     *                      <li>{@code steps.ttl}: A description of the transitions from one state to another, called steps.</li>
     *                      <li>{@code goalStates.txt}: The identifiers (IRIs) of end state in the workflow, one per line.</li>
     *                      </ul>
     * @param outputDir     The path to the output directory. If not given, it will be {@code <inputDir>/output}.
     *                      The output directory contains representations of the generated plan(s) (i.e., next steps to take)
     *                      and the context when reasoning is applied.
     *                      After a run, it contains the following files:
     *                      <ul>
     *                      <li>{@code plan.txt}: textual representation of the plan</li>
     *                      <li>{@code plan.mmd}: Mermaid state diagram of the plan</li>
     *                      <li>{@code plan.ttl}: P-Plan representation of the plan</li>
     *                      <li>{@code context_after_reasoning.ttl}: The context after applying the reasoning rules found in the {@code reasoningFile} parameter.</li>
     *                      </ul>
     * @param eyeBin        The command to run eyeling. This can be a path to the eyeling javascript file. Defaults to "eye" if not given.
     * @param reasoningFile Optional. The path to a file containing N3 reasoning rules. These will be applied on the context before generating the workflow plan.
     * @param contextFile   Optional. The path to the current context (i.e., data reflecting the current situation).
     *                      If not given, a run will be performed for every file {@code data_*.ttl}, in alphabetical order.
     *                      If that's the case, the output directory contains subdirectories for every run.
     */
    public static void run(@NonNull String inputDir,
                           @Nullable String outputDir,
                           @Nullable String eyeBin,
                           @Nullable String reasoningFile,
                           @Nullable String contextFile) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        Path scenarioDir = Path.of(inputDir).toAbsolutePath();
        Path shapesPath = scenarioDir.resolve("shapes.ttl");
        Path statesPath = scenarioDir.resolve("states.ttl");
        Path stepsPath = scenarioDir.resolve("steps.ttl");
        Path goalStatesPath = scenarioDir.resolve("goalStates.txt");
        Path outRootPath = outputDir != null ? Paths.get(outputDir) : scenarioDir.resolve("output");
        String eyeBinPath = eyeBin != null ? eyeBin : "eye";
        Optional<String> reasoningFilePath = reasoningFile == null ? Optional.empty() : Optional.of(scenarioDir.resolve(reasoningFile).toString());

        // Get context file(s)
        List<String> contextFileNames = new LinkedList<>();
        if (contextFile != null) {
            contextFileNames.add(contextFile);
        } else {
            contextFileNames = Arrays.stream(scenarioDir.toFile().listFiles())
                    .map(File::getName)
                    .filter(name -> name.startsWith("data_"))
                    .sorted()
                    .toList();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("""
                            == API parameters ===
                            ScenarioDir:    {}
                            shapesPath:     {}
                            statesPath:     {}
                            stepsPath:      {}
                            goalStatesPath: {}
                            outRootPath:    {}
                            eyeBinPath:     {}
                            reasoningFilePath: {}
                            contextFileNames:  {}
                            """,
                    scenarioDir, shapesPath, statesPath, stepsPath, goalStatesPath, outRootPath, eyeBinPath, reasoningFilePath.orElse("None"), contextFileNames);
        }

        // Initialize QueryGraph once; re-use it for each context file.
        QueryGraphBuilder queryGraphBuilder = QueryGraphBuilder.create(stepsPath.toString(), goalStatesPath.toString(), shapesPath.toString(), statesPath.toString());
        QueryGraph queryGraph = queryGraphBuilder.build();

        // Loop over context files
        for (String contextFileName : contextFileNames) {
            logger.info("Processing {}", contextFileName);
            Path contextPath = scenarioDir.resolve(contextFileName);

            // Get output directory and create if it doesn't exist
            Path outPath = outRootPath;
            if (contextFileNames.size() > 1) {
                outPath = outRootPath.resolve(contextFileName.substring(0, contextFileName.lastIndexOf('.')));
            }
            Path contextOutputFile = outPath.resolve("context_after_reasoning.ttl");
            Files.createDirectories(contextOutputFile.getParent());

            // Apply reasoning to context, if some n3 file is given. Add these to the context
            String context = Files.readString(contextPath);
            if (reasoningFilePath.isPresent()) {
                String extraTriples = new ReasonerWrapper(eyeBinPath).run(contextPath.toString(), reasoningFilePath.get());
                if (!extraTriples.isEmpty()) {
                    logger.debug("Extra triples found!");
                    context += extraTriples;
                }
            }
            Files.writeString(contextOutputFile, context, StandardCharsets.UTF_8);

            // Finally, compose the workflow! Print it and write to file
            queryGraph.process(contextOutputFile.toString());
            logger.debug("Writing text plan to file");
            PlanRenderer text = new TextPlanRenderer();
            String planStr = text.render(queryGraph);
            Path planOutputFile = outPath.resolve("plan.txt");
            Files.writeString(planOutputFile, planStr, StandardCharsets.UTF_8);
            logger.debug("Writing mermaid plan to file");
            PlanRenderer mermaid = new MermaidPlanRenderer();
            String mmd = mermaid.render(queryGraph);
            Path mmdOutputFile = outPath.resolve("plan.mmd");
            Files.writeString(mmdOutputFile, mmd, StandardCharsets.UTF_8);
            logger.debug("Writing P-Plan to file");
            PlanRenderer pPlan = new PPlanRenderer();
            String plan = pPlan.render(queryGraph);
            Path pplanOutputFile = outPath.resolve("plan.ttl");
            Files.writeString(pplanOutputFile, plan, StandardCharsets.UTF_8);
        }
    }
}

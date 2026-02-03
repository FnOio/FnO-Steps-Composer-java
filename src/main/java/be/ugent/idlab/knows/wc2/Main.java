package be.ugent.idlab.knows.wc2;

import be.ugent.idlab.knows.wc2.graph.QueryGraph;

import java.io.IOException;
import java.nio.file.Path;

public class Main {

    static void main() throws IOException {
        //Path scenarioDir = Path.of("/home/geraldh/projects/2024_bocemon/code/use-case-scenario-c-thermal-fno-steps/scenario_manually/");
        Path scenarioDir = Path.of("/home/geraldh/projects/oslo/oslo-steps-workflow-composer/scenarios/stepsOR");
        Path shapesPath = scenarioDir.resolve("shapes.ttl");
        Path statesPath = scenarioDir.resolve("states.ttl");
        Path stepsPath = scenarioDir.resolve("steps.ttl");
        Path goalStatesPath = scenarioDir.resolve("goalStates.txt");

        Path contextPath = scenarioDir.resolve("data_option1_02.ttl");

        QueryGraphBuilder queryGraphBuilder = QueryGraphBuilder.create(stepsPath.toString(), goalStatesPath.toString(), shapesPath.toString(), statesPath.toString());
        QueryGraph queryGraph = queryGraphBuilder.build();
        queryGraph.process(contextPath.toString());
    }
}

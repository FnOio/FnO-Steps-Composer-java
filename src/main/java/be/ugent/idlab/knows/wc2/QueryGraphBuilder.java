package be.ugent.idlab.knows.wc2;

import be.ugent.idlab.knows.wc2.graph.State;
import be.ugent.idlab.knows.wc2.graph.Step;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class QueryGraphBuilder {
    private final Graph shapesGraph;
    private final Graph stepsGraph;
    private final Graph statesGraph;
    private final Set<Node> goalStates;

    private final Map<String, State> processedStates = new HashMap<>();
    private final Map<String, Step> processedSteps = new HashMap<>();

    private static final Node shaclPropertyPredicate = NodeFactory.createURI("http://www.w3.org/ns/shacl#property");
    private static final Node stateShapePredicate = NodeFactory.createURI("https://w3id.org/imec/ns/fno-steps#hasStateShape");
    private static final Node producesStatePredicate = NodeFactory.createURI("https://w3id.org/imec/ns/fno-steps#producesState");
    private static final Node requiresStatePredicate = NodeFactory.createURI("https://w3id.org/imec/ns/fno-steps#requiresState");
    private static final Node typePredicate = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

    private static final Node journeyLevelStepClass = NodeFactory.createURI("https://w3id.org/imec/ns/fno-steps#JourneyLevelStep");
    private static final Node containerLevelStepClass = NodeFactory.createURI("https://w3id.org/imec/ns/fno-steps#ContainerLevelStep");

    private static final Node emptyState = NodeFactory.createURI("https://w3id.org/imec/ns/fno-steps#emptyState");

    private QueryGraphBuilder(Graph shapesGraph, Graph stepsGraph, Graph statesGraph, Set<Node> goalStates) {
        this.shapesGraph = shapesGraph;
        this.stepsGraph = stepsGraph;
        this.statesGraph = statesGraph;
        this.goalStates = goalStates;
    }

    public static QueryGraphBuilder create(
            final String shapesPath,
            final String statesPath,
            final String stepsPath,
            final String goalStatesPath
    ) throws IOException {
        Graph shapesGraph = RDFDataMgr.loadGraph(shapesPath);
        Graph stepsGraph = RDFDataMgr.loadGraph(stepsPath);
        Graph statesGraph = RDFDataMgr.loadGraph(statesPath);
        // Read the goal states
        Set<Node> goalStates = readGoalStates(goalStatesPath);
        return new QueryGraphBuilder(shapesGraph, stepsGraph, statesGraph, goalStates);
    }

    private static Set<Node> readGoalStates(final String goalStatesFile) throws IOException {
        Set<Node> goalStates = new HashSet<>();
        List<String> lines = Files.readAllLines(Path.of(goalStatesFile), StandardCharsets.UTF_8);
        for (String line : lines) {
            line = line.trim();
            if (!line.isBlank() && !line.startsWith("#")) {
                Node goalStateNode = NodeFactory.createURI(line);
                goalStates.add(goalStateNode);
            }
        }
        return goalStates;
    }

    public void build() {
        // Ignore journey- and container level steps; so here component steps must form paths...
        deleteNodesOfType(stepsGraph, journeyLevelStepClass);
        deleteNodesOfType(stepsGraph, containerLevelStepClass);

        // first build graph
        for (Node goalState : goalStates) {
            processPathsBackwards(goalState, 0);
        }

        // then apply the right operators
        for (Node goalStateNode : goalStates) {
            State goalState = processedStates.get(goalStateNode.getURI());
            goalState.fixOperator();
        }

        System.out.println("Done!");
    }

    private void processPathsBackwards(final Node currentStateNode, final int stackDepth) {
        int indentation = stackDepth * 2;
        System.out.println(("* currentState = " + currentStateNode.getURI()).indent(indentation));
        String currentStateIRI = currentStateNode.getURI();
        State currentState = processedStates.computeIfAbsent(currentStateIRI, State::new);

        if (currentStateNode.equals(emptyState)) {
            // done!
            return;
        }

        List<Triple> incomingSteps = stepsGraph.find(Node.ANY, producesStatePredicate, currentStateNode).toList();
        for (Triple incomingStep : incomingSteps) {
            Node previousStepNode = incomingStep.getSubject();
            System.out.println(("previousStep = " + previousStepNode.getURI()).indent(indentation));
            Step previousStep = processedSteps.computeIfAbsent(previousStepNode.getURI(), Step::new);
            currentState.setPreviousStep(previousStep);
            for (ExtendedIterator<Triple> previousStateIter = stepsGraph.find(previousStepNode, requiresStatePredicate, Node.ANY); previousStateIter.hasNext(); ) {
                Triple previousStateTriple = previousStateIter.next();
                Node previousStateNode = previousStateTriple.getObject();
                String previousStateIRI = previousStateNode.getURI();
                State previousState = processedStates.computeIfAbsent(previousStateIRI, State::new);
                previousState.setNextStep(previousStep);
                processPathsBackwards(previousStateNode, stackDepth + 1);
            }

        }
    }

    private static void deleteNodesOfType(final Graph graph, final Node classNode) {
        // first find subject(s) with certain class
        Set<Node> nodesToDelete = new HashSet<>();
        for (ExtendedIterator<Triple> typeTriplesIter = graph.find(Node.ANY, typePredicate, classNode); typeTriplesIter.hasNext(); ) {
            Triple triple = typeTriplesIter.next();
            nodesToDelete.add(triple.getSubject());
        }
        // remove all triples with matching subjects
        for (Node node : nodesToDelete) {
            System.out.println("Deleting node " + node.getURI());
            graph.remove(node, Node.ANY, Node.ANY);
        }
    }
}

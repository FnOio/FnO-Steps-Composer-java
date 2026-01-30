package be.ugent.idlab.knows.wc2;

import be.ugent.idlab.knows.wc2.graph.Operator;
import be.ugent.idlab.knows.wc2.graph.State;
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

import static be.ugent.idlab.knows.wc2.graph.Operator.*;


public class QueryGraphBuilder {
    private final Graph stepsGraph;
    private final Set<String> goalStates;

    private final Map<String, State> processedStates = new HashMap<>();

    private static final Node producesStatePredicate = NodeFactory.createURI("https://w3id.org/imec/ns/fno-steps#producesState");
    private static final Node requiresStatePredicate = NodeFactory.createURI("https://w3id.org/imec/ns/fno-steps#requiresState");
    private static final Node typePredicate = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

    private static final Node journeyLevelStepClass = NodeFactory.createURI("https://w3id.org/imec/ns/fno-steps#JourneyLevelStep");
    private static final Node containerLevelStepClass = NodeFactory.createURI("https://w3id.org/imec/ns/fno-steps#ContainerLevelStep");

    private static final Node emptyState = NodeFactory.createURI("https://w3id.org/imec/ns/fno-steps#emptyState");

    private QueryGraphBuilder(Graph stepsGraph, Set<String> goalStates) {
        this.stepsGraph = stepsGraph;
        this.goalStates = goalStates;
    }

    public static QueryGraphBuilder create(
            final String stepsPath,
            final String goalStatesPath
    ) throws IOException {
        Graph stepsGraph = RDFDataMgr.loadGraph(stepsPath);
        // Read the goal states
        Set<String> goalStates = readGoalStates(goalStatesPath);
        return new QueryGraphBuilder(stepsGraph, goalStates);
    }

    private static Set<String> readGoalStates(final String goalStatesFile) throws IOException {
        Set<String> goalStates = new HashSet<>();
        List<String> lines = Files.readAllLines(Path.of(goalStatesFile), StandardCharsets.UTF_8);
        for (String line : lines) {
            line = line.trim();
            if (!line.isBlank() && !line.startsWith("#")) {
                goalStates.add(line);
            }
        }
        return goalStates;
    }

    public void build() {
        // Ignore journey- and container level steps; so here component steps must form paths...
        deleteNodesOfType(stepsGraph, journeyLevelStepClass);
        deleteNodesOfType(stepsGraph, containerLevelStepClass);

        // first build graph
        for (String goalStateIri : goalStates) {
            Node goalState = NodeFactory.createURI(goalStateIri);
            processPathsBackwards(goalState, new Stack<>(), null, null);
        }
    }

    private void processPathsBackwards(final Node currentStateNode,
                                       final Stack<Operator> operators,
                                       final String nextStepIRI,
                                       final State nextState
    ) {

        String currentStateIRI = currentStateNode.getURI();
        boolean currentStateAlreadyVisited = false;
        State currentState;
        if (processedStates.containsKey(currentStateIRI)) {
            currentState = processedStates.get(currentStateIRI);
            currentStateAlreadyVisited = true;
        } else {
            currentState = new State(currentStateIRI);
            processedStates.put(currentStateIRI, currentState);
        }

        if (nextStepIRI != null && nextState != null) {
            currentState.addNextState(nextStepIRI, nextState);
        }

        if (currentStateAlreadyVisited) {
            return;
        }

        // check if there are multiple outgoing steps
        List<Triple> outgoingSteps = stepsGraph.find(Node.ANY, requiresStatePredicate, currentStateNode).toList();
        if (outgoingSteps.size() > 1) {
            Operator operator = operators.empty() ? XOR : operators.pop();
            currentState.setOperator(operator);
        }

        // The recursion ends when we reached the start state, emptyState!
        if (currentStateNode.equals(emptyState)) {
            // done!
            return;
        }

        List<Triple> incomingSteps = stepsGraph.find(Node.ANY, producesStatePredicate, currentStateNode).toList();

        // Check if there are multiple incoming steps; this means the operator is XOR
        // where the original path split.
        if (incomingSteps.size() > 1) {
            operators.push(XOR);
            currentState.setEndOfOperator(XOR);
        } else {
            // AND when incoming step has more start (requires) states
            Node incomingStep = incomingSteps.getFirst().getSubject();
            int startStatesForStep = stepsGraph.find(incomingStep, requiresStatePredicate, Node.ANY).toList().size();
            if (startStatesForStep > 1) {
                operators.push(AND);
                currentState.setEndOfOperator(AND);
            }
        }

        // Go to next recursive iteration by going the path(s) backward!
        for (Triple incomingStep : incomingSteps) {
            Node previousStep = incomingStep.getSubject();
            String previousStepIRI = previousStep.getURI();
            for (ExtendedIterator<Triple> previousStateIter = stepsGraph.find(previousStep, requiresStatePredicate, Node.ANY); previousStateIter.hasNext(); ) {
                Triple previousStateTriple = previousStateIter.next();
                processPathsBackwards(previousStateTriple.getObject(), operators, previousStepIRI, currentState);
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

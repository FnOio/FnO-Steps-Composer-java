package be.ugent.idlab.knows.wc2;

import be.ugent.idlab.knows.wc2.graph.Operator;
import be.ugent.idlab.knows.wc2.graph.State;
import be.ugent.idlab.knows.wc2.graph.State2;
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

import static be.ugent.idlab.knows.wc2.graph.Operator.*;


public class QueryGraphBuilder {
    private final Graph shapesGraph;
    private final Graph stepsGraph;
    private final Graph statesGraph;
    private final Set<String> goalStates;

    private final Map<String, State> processedStates = new HashMap<>();
    private final Map<String, State2> processedStates2 = new HashMap<>();
    private final Map<String, Step> processedSteps = new HashMap<>();

    private static final Node shaclPropertyPredicate = NodeFactory.createURI("http://www.w3.org/ns/shacl#property");
    private static final Node stateShapePredicate = NodeFactory.createURI("https://w3id.org/imec/ns/fno-steps#hasStateShape");
    private static final Node producesStatePredicate = NodeFactory.createURI("https://w3id.org/imec/ns/fno-steps#producesState");
    private static final Node requiresStatePredicate = NodeFactory.createURI("https://w3id.org/imec/ns/fno-steps#requiresState");
    private static final Node typePredicate = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

    private static final Node journeyLevelStepClass = NodeFactory.createURI("https://w3id.org/imec/ns/fno-steps#JourneyLevelStep");
    private static final Node containerLevelStepClass = NodeFactory.createURI("https://w3id.org/imec/ns/fno-steps#ContainerLevelStep");

    private static final Node emptyState = NodeFactory.createURI("https://w3id.org/imec/ns/fno-steps#emptyState");

    private QueryGraphBuilder(Graph shapesGraph, Graph stepsGraph, Graph statesGraph, Set<String> goalStates) {
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
        Set<String> goalStates = readGoalStates(goalStatesPath);
        return new QueryGraphBuilder(shapesGraph, stepsGraph, statesGraph, goalStates);
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
            //processPathsBackwards(goalState, true, 0);
            processPathsBackwards2(goalState, new Stack<>(), null, null);
        }

/*        // then apply the right operators
        for (String goalStateIri : goalStates) {
            State goalState = processedStates.get(goalStateIri);
            goalState.fixOperator();
            goalState.pushBackGoalState();
        }*/

        System.out.println("Done!");
    }

    private void processPathsBackwards2(final Node currentStateNode,
                                        final Stack<Operator> operators,
                                        final String nextStepIRI,
                                        final State2 nextState
    ) {
        int indentation = operators.size() * 2;
        String currentStateIRI = currentStateNode.getURI();
        System.out.println(("* currentState = " + currentStateIRI).indent(indentation));
        System.out.println(("  operators: " + operators).indent(indentation));

        boolean currentStateAlreadyVisited = false;

        State2 currentState;
        if (processedStates2.containsKey(currentStateIRI)) {
            currentState = processedStates2.get(currentStateIRI);
            currentStateAlreadyVisited = true;
        } else {
            currentState = new State2(currentStateIRI);
            processedStates2.put(currentStateIRI, currentState);
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
            System.out.println(("Split! Operator: " + operator).indent(indentation));
            // subPath...
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
            System.out.println(("previousStep = " + previousStepIRI).indent(indentation));
            for (ExtendedIterator<Triple> previousStateIter = stepsGraph.find(previousStep, requiresStatePredicate, Node.ANY); previousStateIter.hasNext(); ) {
                Triple previousStateTriple = previousStateIter.next();
                processPathsBackwards2(previousStateTriple.getObject(), operators, previousStepIRI, currentState);
            }
        }
    }

    private void processPathsBackwards(final Node currentStateNode, boolean isGoalState, final int stackDepth) {
        int indentation = stackDepth * 2;
        System.out.println(("* currentState = " + currentStateNode.getURI()).indent(indentation));
        String currentStateIRI = currentStateNode.getURI();
        State currentState = processedStates.computeIfAbsent(currentStateIRI,iri -> new State(iri, isGoalState));
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
                processPathsBackwards(previousStateNode, false, stackDepth + 1);
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

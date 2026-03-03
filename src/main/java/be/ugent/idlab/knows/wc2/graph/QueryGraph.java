package be.ugent.idlab.knows.wc2.graph;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.sparql.graph.GraphFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static be.ugent.idlab.knows.wc2.graph.Operator.AND;
import static be.ugent.idlab.knows.wc2.graph.Operator.XOR;

public class QueryGraph {
    private final static Logger logger = LoggerFactory.getLogger(QueryGraph.class);

    private final Map<String, State> states;
    private final Map<String, String> shapeToState;

    private final Shapes shapes;

    // used for generating state names when outputting mermaid
    private int generatedStateNr = 0;

    public QueryGraph(final Map<String, State> states,
                      final Graph shapesGraph,
                      final Map<String, String> shapeToState) {
        this.states = states;
        this.shapeToState = shapeToState;
        shapes = Shapes.parse(shapesGraph);
    }

    /**
     * Calculate matching states, next steps to take, ...
     * @param contextFile   The context data + reasoned knowledge
     */
    public void process(final String contextFile) {
        Graph context = RDFDataMgr.loadGraph(contextFile);

        // Reset the status of all States in the graph
        State startState = states.get("https://w3id.org/imec/ns/fno-steps#emptyState");
        resetStatus(startState);

        // Which shapes/states DO NOT match the context (invalid)?
        Set<String> nonMatchingShapes = new HashSet<>();
        ValidationReport report = ShaclValidator.get().validate(shapes, context);
        for (ReportEntry entry : report.getEntries()) {
            Node condextNode = entry.focusNode(); // The matching context node
            Node shapeNode = entry.source();
            logger.debug("{} does NOT match {}", condextNode, shapeNode);
            nonMatchingShapes.add(shapeNode.getURI());
        }

        // Mark nodes in the graph with their status
        Set<String> matchingShapes = new HashSet<>(shapeToState.keySet());
        if (report.getEntries().isEmpty()) {
            logger.debug("No matching shapes found");
            matchingShapes.clear();
        } else {
            // just remove the non-matching shapes
            matchingShapes.removeAll(nonMatchingShapes);
        }
        Set<String> matchingStates = matchingShapes.stream().map(shapeToState::get).collect(Collectors.toSet());
        // add the emptyState (start state) to the matching states
        matchingStates.add("https://w3id.org/imec/ns/fno-steps#emptyState");

        logger.debug("Matching states: {}\n", matchingStates);
        for (String matchingState : matchingStates) {
            State state = states.get(matchingState);
            markPreviousStates(state, Status.Current, null);
        }
    }

    private void markPreviousStates(State currentState, Status status, State nextState) {
        if (currentState.getStatus() == Status.Deleted) {
            return;
        }
        currentState.mark(status);
        if (currentState.getOperator().equals(XOR) && nextState != null) {
            // first get all other "outgoing" states
            Set<State> statesToRemove = new HashSet<>(currentState.getNextSteps().values());
            statesToRemove.remove(nextState);
            for (State state : statesToRemove) {
                // prune these states until end of XOR or end state
                pruneForwardUntilEndXOR(state, 0);
            }

        }
        Status statusToPass = status;
        if (status == Status.Done || currentState.getStatus() == Status.Current) {
            statusToPass = Status.Done;
        }
        for (State previousState : currentState.getPreviousStates()) {
            markPreviousStates(previousState, statusToPass, currentState);
        }
    }

    private void pruneForwardUntilEndXOR(State currentState, int xorLevel) {
        if (currentState.getNextSteps().isEmpty()) {
            currentState.setStatus(Status.Deleted);
            return;
        }
        if (currentState.getOperator().equals(XOR)) {
            xorLevel++;
        } else if (currentState.getEndOfOperator().equals(XOR)) {
            if (xorLevel == 0) {
                return;
            } else {
                xorLevel--;
            }
        }
        currentState.setStatus(Status.Deleted);
        for (State nextState : currentState.getNextSteps().values()) {
            pruneForwardUntilEndXOR(nextState, xorLevel);
        }
    }

    private void resetStatus(State state) {
        state.setStatus(Status.Todo);
        for (State nextState : state.getNextSteps().values()) {
            resetStatus(nextState);
        }
    }

    public String printPlan() {
        StringBuilder outStr = new StringBuilder();
        State currentState = states.get("https://w3id.org/imec/ns/fno-steps#emptyState");
        outStr.append(printPlan(currentState, 0));
        return outStr.toString();
    }

    private String printPlan(State currentState, int level) {
        StringBuilder outStr = new StringBuilder();
        int nextLevel = level;
        String currentStateStr = currentState.name();
        switch (currentState.getStatus()) {
            case Done -> currentStateStr += " ☑";
            case Current -> currentStateStr += " ◉";
            case Deleted -> currentStateStr += " ☒";
        }
        outStr.append(("State: " + currentStateStr).indent(level * 2));
        switch (currentState.getOperator()) {
            case XOR -> {
                outStr.append("OR".indent(level * 2));
                nextLevel++;
            }
            case AND -> {
                outStr.append("AND".indent(level * 2));
                nextLevel++;
            }
        }
        for (Map.Entry<String, State> stringStateEntry : currentState.getNextSteps().entrySet()) {
            String nextStepIRI =  stringStateEntry.getKey();
            String nextStep = nextStepIRI.substring(nextStepIRI.lastIndexOf('#') + 1);
            State nextState  = stringStateEntry.getValue();

            String stepStatusString = "";
            switch (nextState.getStatus()) {
                case Deleted -> stepStatusString = " ☒";
                case Done, Current -> {
                    if (currentState.getStatus() == Status.Deleted) {
                        stepStatusString = " ☒";
                    } else {
                        stepStatusString = " ☑";
                    }
                }
                case Todo ->  stepStatusString = " ☐";
            }

            outStr.append(("Step:  " + nextStep + stepStatusString).indent(level * 2));
            outStr.append(printPlan(nextState, nextLevel));
        }
        return outStr.toString();
    }

    public String toMermaid() {
        StringBuilder out = new StringBuilder("stateDiagram-v2\n\n");
        out.append("classDef current font-weight:bold,fill:blue,color:white\n")
                .append("classDef done fill:#0a0,color:white\n")
                .append("classDef deleted fill:#f55,color:white\n\n");
        State startState = states.get("https://w3id.org/imec/ns/fno-steps#emptyState");
        out.append(toMermaid(startState));
        return out.toString();
    }

    private String toMermaid(final State state) {
        StringBuilder out = new StringBuilder();

        String stateName;
        if (state.getPreviousStates().isEmpty()) {
            stateName = "[*]";
        }
        else {
            stateName = state.name();
            switch (state.getStatus()) {
                case Deleted -> out.append("class ").append(stateName).append(" deleted\n");
                case Done -> out.append("class ").append(stateName).append(" done\n");
                case Current -> out.append("class ").append(stateName).append(" current\n");
            }
        }

        switch (state.getOperator()) {
            case XOR -> {
                // Add "choice" state
                String generatedStateName = "choice_" + generatedStateNr++;
                out.append("state ").append(generatedStateName).append(" <<choice>>\n");
                out.append(stateName).append(" --> ").append(generatedStateName).append('\n');
                stateName = generatedStateName;
            }
            case AND -> {
                // Add "fork" state
                String generatedStateName = "fork_" + generatedStateNr++;
                out.append("state ").append(generatedStateName).append(" <<fork>>\n");
                out.append(stateName).append(" --> ").append(generatedStateName).append('\n');
                stateName = generatedStateName;
            }
            default -> { /* nothing */ }
        }

        Map<String, State> nextSteps = state.getNextSteps();
        if (nextSteps.isEmpty()) {
            out.append(stateName).append(" --> ").append("[*]\n");
        } else {
            for (Map.Entry<String, State> stringStateEntry : state.getNextSteps().entrySet()) {
                String nextStepIRI = stringStateEntry.getKey();
                String nextStep = nextStepIRI.substring(nextStepIRI.lastIndexOf('#') + 1);
                State nextState = stringStateEntry.getValue();
                String nextStateName = nextState.name();
                if (nextState.getEndOfOperator().equals(AND)) {
                    // Add "join" state
                    String generatedStateName = "join_" + generatedStateNr++;
                    out.append("state ").append(generatedStateName).append(" <<join>>\n");
                    out.append(stateName).append(" --> ").append(generatedStateName).append('\n');
                    stateName = generatedStateName;
                }
                out.append(stateName).append(" --> ").append(nextStateName)
                        .append(" : ").append(nextStep).append('\n');
                out.append(toMermaid(nextState));
            }
        }

        return out.toString();
    }

    // Some stuff to help output to P-Plan
    private final static String baseIRI = "http://localhost:8000/pplan#";
    private final static String ppPrefix = "http://purl.org/net/p-plan#";
    private final static String fnoStepsPrefix = "https://w3id.org/imec/ns/fno-steps#";

    // subjects / objects
    private final static Node thePlan = NodeFactory.createURI(baseIRI + "pplan");
    private final static Node ppClass = NodeFactory.createURI(ppPrefix + "Plan");

    // predicates
    private final static Node a = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
    private final static Node stepOfPlan = NodeFactory.createURI(ppPrefix + "isStepOfPlan");
    private final static Node usesStep = NodeFactory.createURI(fnoStepsPrefix + "usesStep");
    private final static Node isPrecededBy = NodeFactory.createURI(fnoStepsPrefix + "isPrecededBy");


    public String toPPlan() {
        Graph planGraph = GraphFactory.createDefaultGraph();
        planGraph.add(thePlan, a, ppClass);
        // TODO: cost?
        State startState = states.get("https://w3id.org/imec/ns/fno-steps#emptyState");
        toPPlan(planGraph, startState, null);

        StringWriter out = new StringWriter();
        RDFDataMgr.write(out, planGraph, RDFFormat.TURTLE_BLOCKS);
        return out.toString();
    }

    private void toPPlan(final Graph planGraph, final State state, final Node previousStep) {
        for (Map.Entry<String, State> nextStep : state.getNextSteps().entrySet()) {
            String fnoStepsIRI = nextStep.getKey();
            Node fnoStep =  NodeFactory.createURI(fnoStepsIRI);
            String ppStepIRI =  fnoStepsIRI.substring(fnoStepsIRI.lastIndexOf('#') + 1);
            Node stepNode = NodeFactory.createURI(baseIRI + ppStepIRI);
            planGraph.add(stepNode, stepOfPlan, thePlan);
            planGraph.add(stepNode, usesStep, fnoStep);
            if (previousStep != null) {
                planGraph.add(stepNode, isPrecededBy, previousStep);
            }
            State nextState =  nextStep.getValue();
            toPPlan(planGraph, nextState, stepNode);
        }
    }
}

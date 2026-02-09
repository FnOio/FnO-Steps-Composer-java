package be.ugent.idlab.knows.wc2.graph;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.validation.ReportEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static be.ugent.idlab.knows.wc2.graph.Operator.XOR;

public class QueryGraph {
    private final static Logger logger = LoggerFactory.getLogger(QueryGraph.class);

    private final Map<String, State> states;
    private final Map<String, String> shapeToState;

    private final Shapes shapes;

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
     * @return A textual representation of the workflow.
     */
    public String process(final String contextFile) {
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
        matchingShapes.removeAll(nonMatchingShapes);
        Set<String> matchingStates = matchingShapes.stream().map(shapeToState::get).collect(Collectors.toSet());
        // add the emptyState (start state) to the matching states
        matchingStates.add("https://w3id.org/imec/ns/fno-steps#emptyState");

        logger.debug("Matching states: {}\n", matchingStates);
        for (String matchingState : matchingStates) {
            State state = states.get(matchingState);
            pruneGraph(state, null);
            markPreviousStates(state, Status.Current);
        }

        // Generate textual representation of the plan (graph)
        return printPlan();
    }

    private void pruneGraph(State currentState, State previousState) {
        if (currentState.getStatus().equals(Status.Deleted)) {
            return;
        }
        if (currentState.getOperator().equals(XOR)) {
            return;
        }
        if (currentState.getEndOfOperator().equals(XOR) && previousState != null) {
            // prune!
            // first get all other "incoming" states
            Set<State> prevStatesToRemove = new HashSet<>(currentState.getPreviousStates());
            prevStatesToRemove.remove(previousState);
            // prune these states until start of XOR (should be max 1)
            for (State prevState : prevStatesToRemove) {
                pruneBackwardsTillXOR(prevState, 0);
            }
            return;
        }
        for (State nextState : currentState.getNextSteps().values()) {
            pruneGraph(nextState, currentState);
        }
    }

    private void pruneBackwardsTillXOR(State currentState, int xorLevel) {
        if (currentState.getEndOfOperator().equals(XOR)) {
            xorLevel++;
        } else if (currentState.getOperator().equals(XOR)) {
            if (xorLevel == 0) {
                return;
            } else {
                xorLevel--;
            }
        }
        currentState.setStatus(Status.Deleted);
        for (State previousState : currentState.getPreviousStates()) {
            pruneBackwardsTillXOR(previousState, xorLevel);
        }
    }

    private void markPreviousStates(State currentState, Status status) {
        if (currentState.getStatus() == Status.Deleted) {
            return;
        }
        currentState.mark(status);

        Status statusToPass = status;
        if (status == Status.Done || currentState.getStatus() == Status.Current) {
            statusToPass = Status.Done;
        }
        for (State previousState : currentState.getPreviousStates()) {
            markPreviousStates(previousState, statusToPass);
        }
    }

    private void resetStatus(State state) {
        state.setStatus(Status.Todo);
        for (State nextState : state.getNextSteps().values()) {
            resetStatus(nextState);
        }
    }

    private String printPlan() {
        StringBuilder outStr = new StringBuilder();
        State currentState = states.get("https://w3id.org/imec/ns/fno-steps#emptyState");
        // print current state(s)
        outStr.append("Current state(s):\n");
        states.values().stream().filter(state -> state.getStatus() == Status.Current).forEach(state -> outStr.append(state).append('\n'));

        outStr.append("\nSteps:\n");
        outStr.append(printPlan(currentState, 0));
        return outStr.toString();
    }

    private String printPlan(State currentState, int level) {
        StringBuilder outStr = new StringBuilder();
        int nextLevel = level;
        String currentStateStr = currentState.toString();
        switch (currentState.getStatus()) {
            case Done -> currentStateStr += " ☑";
            case Current -> currentStateStr += " ☐";
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
            String nextStep =  stringStateEntry.getKey();
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
}

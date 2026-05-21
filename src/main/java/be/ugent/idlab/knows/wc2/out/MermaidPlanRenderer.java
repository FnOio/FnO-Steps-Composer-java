package be.ugent.idlab.knows.wc2.out;

import be.ugent.idlab.knows.wc2.graph.QueryGraph;
import be.ugent.idlab.knows.wc2.graph.State;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static be.ugent.idlab.knows.wc2.graph.Operator.AND;

public class MermaidPlanRenderer implements PlanRenderer {
    private int generatedStateNr = 0;
    private final Map<String, Integer> stateWithJoinState = new HashMap<>();
    private final Set<State> processedStates = new HashSet<>();

    @Override
    public String render(final QueryGraph queryGraph) {
        generatedStateNr = 0;
        stateWithJoinState.clear();
        processedStates.clear();
        StringBuilder out = new StringBuilder("stateDiagram-v2\n\n");
        out.append("classDef current font-weight:bold,fill:blue,color:white\n")
                .append("classDef done fill:#0a0,color:white\n")
                .append("classDef deleted fill:#f55,color:white\n\n");
        State startState = queryGraph.getStartState();
        out.append(toMermaid(startState));
        return out.toString();
    }

    private String toMermaid(final State state) {
        if (processedStates.contains(state)) {
            return "";
        } else {
            processedStates.add(state);
        }

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
                boolean skip = false;   // Turns "true" if path after a join has already been processed
                if (nextState.getEndOfOperator().equals(AND)) {
                    String joinStateName = "join_";
                    if (stateWithJoinState.containsKey(nextStateName)) {
                        joinStateName += stateWithJoinState.get(nextStateName);
                        skip = true;
                    } else {
                        // Add "join" state
                        stateWithJoinState.put(nextStateName, generatedStateNr);
                        joinStateName += generatedStateNr++;
                    }
                    out.append("state ").append(joinStateName).append(" <<join>>\n");
                    out.append(stateName).append(" --> ").append(joinStateName).append('\n');
                    stateName = joinStateName;
                }
                if (!skip) {
                    out.append(stateName).append(" --> ").append(nextStateName)
                            .append(" : ").append(nextStep).append('\n');
                    out.append(toMermaid(nextState));
                }
            }
        }

        return out.toString();
    }
}

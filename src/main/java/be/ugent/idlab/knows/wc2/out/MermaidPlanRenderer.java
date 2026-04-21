package be.ugent.idlab.knows.wc2.out;

import be.ugent.idlab.knows.wc2.graph.QueryGraph;
import be.ugent.idlab.knows.wc2.graph.State;

import java.util.Map;

import static be.ugent.idlab.knows.wc2.graph.Operator.AND;

public class MermaidPlanRenderer implements PlanRenderer {
    private int generatedStateNr = 0;

    @Override
    public String render(final QueryGraph queryGraph) {
        generatedStateNr = 0;
        StringBuilder out = new StringBuilder("stateDiagram-v2\n\n");
        out.append("classDef current font-weight:bold,fill:blue,color:white\n")
                .append("classDef done fill:#0a0,color:white\n")
                .append("classDef deleted fill:#f55,color:white\n\n");
        State startState = queryGraph.getStates().get("https://w3id.org/imec/ns/fno-steps#emptyState");
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
}

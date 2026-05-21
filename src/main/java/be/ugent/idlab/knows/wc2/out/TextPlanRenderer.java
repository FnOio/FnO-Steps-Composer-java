package be.ugent.idlab.knows.wc2.out;

import be.ugent.idlab.knows.wc2.graph.QueryGraph;
import be.ugent.idlab.knows.wc2.graph.State;
import be.ugent.idlab.knows.wc2.graph.Status;

import java.util.Map;

public class TextPlanRenderer implements PlanRenderer {
    @Override
    public String render(final QueryGraph queryGraph) {
        StringBuilder outStr = new StringBuilder();
        State currentState = queryGraph.getStartState();
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
                case Todo ->  {
                    if (currentState.getStatus() == Status.Deleted) {
                        stepStatusString = " ☒";
                    } else {
                        stepStatusString = " ☐";
                    }
                }
            }

            outStr.append(("Step:  " + nextStep + stepStatusString).indent(level * 2));
            outStr.append(printPlan(nextState, nextLevel));
        }
        return outStr.toString();
    }
}

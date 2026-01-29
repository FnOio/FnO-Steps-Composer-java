package be.ugent.idlab.knows.wc2.graph;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Step {
    private final String iri;
    private final Set<State> nextStates = new HashSet<>();
    private final Set<State> previousStates = new HashSet<>();
    private final Set<State> toGoalStates = new HashSet<>();

    public Step(String iri) {
        this.iri = iri;
    }

    public void setNextState(State state) {
        nextStates.add(state);
    }

    public void setPreviousState(State state) {
        previousStates.add(state);
    }

    public boolean hasMultiplePreviousStates() {
        return previousStates.size() > 1;
    }

    void pushBackOperator(final Operator operator) {
        for (State previousState : previousStates) {
            previousState.fixOperator(operator);
        }
    }

    void pushBackGoalState(State goalState) {
        toGoalStates.add(goalState);
        for (State previousState : previousStates) {
            previousState.pushBackGoalState(goalState);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Step step = (Step) o;
        return Objects.equals(iri, step.iri);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(iri);
    }

    @Override
    public String toString() {
        return iri;
    }
}

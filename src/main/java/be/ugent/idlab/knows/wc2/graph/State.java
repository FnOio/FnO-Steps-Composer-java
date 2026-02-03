package be.ugent.idlab.knows.wc2.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static be.ugent.idlab.knows.wc2.graph.Operator.None;

public class State {
    /**
     * The unique identifier of the state.
     */
    private final String iri;  // the IRI

    /**
     * When a path in the graph splits (i.e. multiple next steps),
     * this operator decides how to follow the next steps
     */
    private Operator operator = None;

    /**
     * Here two paths come together and this operator is of the split
     * of these paths.
     * Not sure if this is needed :)
     */
    private Operator endOfOperator = None;

    private Status status = Status.Todo;

    /**
     * This map contains the next steps and states.
     * A step IRI is mapped to a State object.
     */
    private final Map<String, State> nextSteps = new HashMap<>();
    private final Set<State> previousSteps = new HashSet<>();

    public State(String iri) {
        this.iri = iri;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public void setEndOfOperator(Operator operator) {
        this.endOfOperator = operator;
    }

    public void addNextState(final String stepIRI, final State nextState) {
        nextSteps.put(stepIRI, nextState);
    }

    public void addpreviousState(final State previousState) {
        previousSteps.add(previousState);
    }

    public void mark(Status status) {
        this.status = this.status.getHighestStatus(status);
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isGoal() {
        return nextSteps.isEmpty();
    }

    public Map<String, State> getNextSteps() {
        return nextSteps;
    }

    public Set<State> getPreviousStates() {
        return previousSteps;
    }

    public Status getStatus() {
        return status;
    }

    public String getIri() {
        return iri;
    }

    @Override
    public String toString() {
        return iri;
    }

    public Operator getOperator() {
        return operator;
    }

    public Operator getEndOfOperator() {
        return endOfOperator;
    }
}

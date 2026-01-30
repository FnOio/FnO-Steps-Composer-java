package be.ugent.idlab.knows.wc2.graph;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * This map contains the next steps and states.
     * A step IRI is mapped to a State object.
     */
    private final Map<String, State> nextSteps = new HashMap<>();

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

    @Override
    public String toString() {
        return iri;
    }
}

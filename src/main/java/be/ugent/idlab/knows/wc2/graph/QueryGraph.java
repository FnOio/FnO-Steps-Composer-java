package be.ugent.idlab.knows.wc2.graph;

import java.util.Map;

public class QueryGraph {
    private final Map<String, State> states;

    public QueryGraph(final Map<String, State> states) {
        this.states = states;
    }
}

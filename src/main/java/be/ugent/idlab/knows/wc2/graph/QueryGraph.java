package be.ugent.idlab.knows.wc2.graph;

import org.apache.jena.graph.Graph;

import java.util.Map;

public class QueryGraph {
    private final Map<String, State> states;
    private final Graph shapesGraph;
    private final Map<String, String> shapeToState;

    public QueryGraph(final Map<String, State> states, final Graph shapesGraph, Map<String, String> shapeToState) {
        this.states = states;
        this.shapesGraph = shapesGraph;
        this.shapeToState = shapeToState;
    }
}

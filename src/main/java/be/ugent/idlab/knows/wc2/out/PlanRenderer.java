package be.ugent.idlab.knows.wc2.out;

import be.ugent.idlab.knows.wc2.graph.QueryGraph;

public interface PlanRenderer {
    String render(final QueryGraph queryGraph);
}

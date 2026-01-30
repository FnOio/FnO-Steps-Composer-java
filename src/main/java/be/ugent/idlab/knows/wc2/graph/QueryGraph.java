package be.ugent.idlab.knows.wc2.graph;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.validation.ReportEntry;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class QueryGraph {
    private final Map<String, State> states;
    private final Graph shapesGraph;
    private final Map<String, String> shapeToState;

    private final Shapes shapes;

    public QueryGraph(final Map<String, State> states,
                      final Graph shapesGraph,
                      final Map<String, String> shapeToState) {
        this.states = states;
        this.shapesGraph = shapesGraph;
        this.shapeToState = shapeToState;
        shapes = Shapes.parse(shapesGraph);
    }

    /**
     * Calculate matching states, next steps to take, ...
     * @param contextFile   The context data + reasoned knowledge
     */
    public void process(final String contextFile) {
        Graph context = RDFDataMgr.loadGraph(contextFile);

        // Which shapes/states DO NOT match the context (invalid)?
        Set<String> nonMatchingShapes = new HashSet<>();
        ValidationReport report = ShaclValidator.get().validate(shapes, context);
        for (ReportEntry entry : report.getEntries()) {
            Node condextNode = entry.focusNode(); // The matching context node
            Node shapeNode = entry.source();
            System.out.println(condextNode.getURI() + " does NOT match " + shapeNode.getURI());
            nonMatchingShapes.add(shapeNode.getURI());
        }

        Set<String> matchingShapes = new HashSet<>(shapeToState.keySet());
        matchingShapes.removeAll(nonMatchingShapes);

        System.out.println("Matching shapes: " + matchingShapes);
    }
}

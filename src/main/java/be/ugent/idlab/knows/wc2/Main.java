package be.ugent.idlab.knows.wc2;

import java.io.IOException;
import java.nio.file.Path;

public class Main {

    static void main() throws IOException {
        Path scenarioDir = Path.of("/home/geraldh/projects/2024_bocemon/code/use-case-scenario-c-thermal-fno-steps/scenario_manually/");
        //Path scenarioDir = Path.of("/home/geraldh/projects/oslo/oslo-steps-workflow-composer/scenarios/stepsOR");
        //Path shapesPath = scenarioDir.resolve("shapes.ttl");
        //Path statePath = scenarioDir.resolve("states.ttl");
        Path stepsPath = scenarioDir.resolve("steps.ttl");
        Path goalStatesPath = scenarioDir.resolve("goalStates.txt");

        QueryGraphBuilder queryGraphBuilder = QueryGraphBuilder.create(stepsPath.toString(), goalStatesPath.toString());
        queryGraphBuilder.build();


//        Map<Node, Node> propertyShapeToState = getPropertyShapeToStateMap(shapesGraph, statesGraph);
//
//
//        Graph dataGraph = RDFDataMgr.loadGraph("/home/geraldh/projects/2024_bocemon/code/use-case-scenario-c-thermal-fno-steps/scenario_manually/data_03_people_requests.ttl");
//        // TODO: enrich data graph with results from knowledge.n3
//
//        Shapes shapes = Shapes.parse(shapesGraph);
//
//        ValidationReport report = ShaclValidator.get().validate(shapes, dataGraph);
//        for (ReportEntry entry : report.getEntries()) {
//            Node focusNode = entry.focusNode(); // room1
//            Node sourceNode = entry.source();   // the shacl property shape
//            System.out.println("focusNode = " + focusNode.getURI() + ";  sourceNode = " + sourceNode.getURI());
//        }
        //ShLib.printReport(report);

    }


//    private static Map<Node, Node> getPropertyShapeToStateMap(final Graph shapesGraph, final Graph statesGraph) {
//        Map<Node, Node> propertyShapeToState = new HashMap<>();
//        // Find which StateShape has which PropertyShape
//        for (ExtendedIterator<Triple> shapeIt = shapesGraph.find(Node.ANY, shaclPropertyPredicate, Node.ANY); shapeIt.hasNext(); ) {
//            Triple shapeTriple = shapeIt.next();
//            Node propertyShapeNode = shapeTriple.getObject();
//            Node stateShapeNode = shapeTriple.getSubject();
//
//            // Find which State has which StateShape, and add to map ProperyShape -> State
//            for (ExtendedIterator<Triple> stateIt = statesGraph.find(Node.ANY, stateShapePredicate, stateShapeNode); stateIt.hasNext(); ) {
//                Triple stateTriple = stateIt.next();    // *should* be one
//                Node stateNode = stateTriple.getSubject();
//                propertyShapeToState.put(propertyShapeNode, stateNode);
//            }
//        }
//        return propertyShapeToState;
//    }
//
}

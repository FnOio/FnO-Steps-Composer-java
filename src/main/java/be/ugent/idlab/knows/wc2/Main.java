package be.ugent.idlab.knows.wc2;

import java.io.IOException;
import java.nio.file.Path;

public class Main {

    static void main(String[] args) throws IOException {
        //Path scenarioDir = Path.of("/home/geraldh/projects/2024_bocemon/code/use-case-scenario-c-thermal-fno-steps/scenario_manually/");
        Path scenarioDir = Path.of("/home/geraldh/projects/oslo/oslo-steps-workflow-composer/scenarios/stepsAND");
        Path shapesPath = scenarioDir.resolve("shapes.ttl");
        Path statePath = scenarioDir.resolve("states.ttl");
        Path stepsPath = scenarioDir.resolve("steps.ttl");
        Path goalStatesPath = scenarioDir.resolve("goalStates.txt");

        QueryGraphBuilder queryGraphBuilder = QueryGraphBuilder.create(shapesPath.toString(), statePath.toString(), stepsPath.toString(), goalStatesPath.toString());
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
//    /**
//     * Builds up paths backwardly, i.e. starting at the end trying to reach the start.
//     * @param currentState
//     * @param statesGraph
//     * @param stepsGraph
//     */
//    private static void getPathsBackwards(
//            final Node currentState,
//            final Graph statesGraph,
//            final Graph stepsGraph,
//            final Stack<Operator> operators,
//            final Map<String, State> stateMap) {
//        int indentation = operators.size() * 2;
//        System.out.println(("* currentState = " + currentState.getURI()).indent(indentation));
//        System.out.println(("  operators: " + operators).indent(indentation));
//
//        // check if there are multiple outgoing steps - wrong, it's STATES
//        List<Triple> outgoingSteps = stepsGraph.find(Node.ANY, requiresStatePredicate, currentState).toList();
//        if (outgoingSteps.size() > 1) {
//            Operator operator = operators.empty() ? OR : operators.pop();
//            System.out.println(("Split! Operator: " + operator).indent(indentation));
//            // subPath...
//        }
//
//        if (currentState.equals(emptyState)) {
//            // done!
//            return;
//        }
//
//        List<Triple> incomingSteps = stepsGraph.find(Node.ANY, producesStatePredicate, currentState).toList();
//        if (incomingSteps.size() > 1) {
//            operators.push(OR);
//        } else {
//            // AND when incoming step has more start (requires) states
//            Node incomingStep = incomingSteps.getFirst().getSubject();
//            int startStatesForStep = stepsGraph.find(incomingStep, requiresStatePredicate, Node.ANY).toList().size();
//            if (startStatesForStep > 1) {
//                operators.push(AND);
//            }
//        }
//
//        for (Triple incomingStep : incomingSteps) {
//            Node previousStep = incomingStep.getSubject();
//            System.out.println(("previousStep = " + previousStep.getURI()).indent(indentation));
//            for (ExtendedIterator<Triple> previousStateIter = stepsGraph.find(previousStep, requiresStatePredicate, Node.ANY); previousStateIter.hasNext(); ) {
//                Triple previousStateTriple = previousStateIter.next();
//                getPathsBackwards(previousStateTriple.getObject(), statesGraph, stepsGraph, operators);
//            }
//
//        }
//    }
//
//    private static Set<Node> readGoalStates(final String goalStatesFile) throws IOException {
//        Set<Node> goalStates = new HashSet<>();
//        List<String> lines = Files.readAllLines(Path.of(goalStatesFile), StandardCharsets.UTF_8);
//        for (String line : lines) {
//            line = line.trim();
//            if (!line.isBlank() && !line.startsWith("#")) {
//                Node goalStateNode = NodeFactory.createURI(line);
//                goalStates.add(goalStateNode);
//            }
//        }
//        return goalStates;
//    }
}

package be.ugent.idlab.knows.wc2.out;

import be.ugent.idlab.knows.wc2.graph.QueryGraph;
import be.ugent.idlab.knows.wc2.graph.State;
import be.ugent.idlab.knows.wc2.graph.Status;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.graph.GraphFactory;

import java.io.StringWriter;
import java.util.Map;

public class PPlanRenderer implements PlanRenderer {

    // Some stuff to help output to P-Plan
    private final static String baseIRI = "http://localhost:8000/pplan#";
    private final static String ppPrefix = "http://purl.org/net/p-plan#";
    private final static String fnoStepsPrefix = "https://w3id.org/imec/ns/fno-steps#";

    // subjects / objects
    private final static Node thePlan = NodeFactory.createURI(baseIRI + "pplan");
    private final static Node ppClass = NodeFactory.createURI(ppPrefix + "Plan");

    // predicates
    private final static Node a = NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
    private final static Node stepOfPlan = NodeFactory.createURI(ppPrefix + "isStepOfPlan");
    private final static Node usesStep = NodeFactory.createURI(fnoStepsPrefix + "usesStep");
    private final static Node isPrecededBy = NodeFactory.createURI(fnoStepsPrefix + "isPrecededBy");


    @Override
    public String render(final QueryGraph queryGraph) {
        Graph planGraph = GraphFactory.createDefaultGraph();
        planGraph.add(thePlan, a, ppClass);
        // TODO: cost?
        // TODO: extension classes (conditionalStep, loopStep, ...)
        // TODO: use P-Plan Entity and P-Plan Activity (an execution of process planned in a Step)

        State currentState = null;
        for (State state : queryGraph.getStates().values()) {
            if (state.getStatus().equals(Status.Current)) {
                currentState = state;
                break;
            }
        }
        if (currentState == null) {
            return "";
        }

        toPPlan(planGraph, currentState, null);
        StringWriter out = new StringWriter();
        RDFDataMgr.write(out, planGraph, RDFFormat.TURTLE_BLOCKS);
        return out.toString();
    }

    private void toPPlan(final Graph planGraph, final State state, final Node previousStep) {
        for (Map.Entry<String, State> nextStep : state.getNextSteps().entrySet()) {

            switch (state.getOperator()) {
                case AND -> {
                    // SplitStep
                    System.out.println();
                }
                case XOR -> {
                    // ConditionalStep
                    // The condition is a certain shape that matches
                    System.out.println();
                    // The "then" and "else" steps are the next steps of this state
                }
            }

            String fnoStepsIRI = nextStep.getKey();
            Node fnoStep =  NodeFactory.createURI(fnoStepsIRI);
            String ppStepIRI =  fnoStepsIRI.substring(fnoStepsIRI.lastIndexOf('#') + 1);
            Node stepNode = NodeFactory.createURI(baseIRI + ppStepIRI);
            planGraph.add(stepNode, stepOfPlan, thePlan);
            planGraph.add(stepNode, usesStep, fnoStep);
            if (previousStep != null) {
                planGraph.add(stepNode, isPrecededBy, previousStep);
            }
            State nextState =  nextStep.getValue();
            toPPlan(planGraph, nextState, stepNode);
        }
    }
}

package be.ugent.idlab.knows.wc2.out;

import be.ugent.idlab.knows.wc2.graph.QueryGraph;
import be.ugent.idlab.knows.wc2.graph.State;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.graph.GraphFactory;

import java.io.StringWriter;
import java.util.*;

import static be.ugent.idlab.knows.wc2.graph.Operator.XOR;

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
        // Split plan into separate plans.
        List<Set<State>> plans = splitPlan(queryGraph);

        Graph planGraph = GraphFactory.createDefaultGraph();
        planGraph.add(thePlan, a, ppClass);
        // TODO: cost?
        // TODO: extension classes (conditionalStep, loopStep, ...)
        // TODO: use P-Plan Entity and P-Plan Activity (an execution of process planned in a Step)

        State currentState = queryGraph.getCurrentState();

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

    /**
     * Splits a plan into separate plans. This happens when there are exclusive OR paths in the plan: each such path
     * is a separate
     * @param queryGraph
     * @return
     */
    private static List<Set<State>> splitPlan(final QueryGraph queryGraph) {
        List<Set<State>> plans = new ArrayList<>(1);
        plans.add(new HashSet<>());
        splitPlan(queryGraph, queryGraph.getCurrentState(), plans);
        return plans;
    }

    private static void splitPlan(final QueryGraph queryGraph, final State currentState, final List<Set<State>> plans) {
        // add current state to latest plan
        Set<State> currentPlan = plans.getLast();
        currentPlan.add(currentState);

        // If the operator is XOR, then a new plan will be created
        if (currentState.getOperator().equals(XOR)) {
            Iterator<State> nextStateIter = currentState.getNextSteps().values().iterator();

            // The first state of the next states can be added to the current plan
            State firstNextState = nextStateIter.next();
            splitPlan(queryGraph, firstNextState, plans);

            // The rest needs new plans
            nextStateIter.forEachRemaining(nextState -> {
                Set<State> newPlan = new HashSet<>();
                addPreviousStatesToPlan(nextState, newPlan);
                plans.add(newPlan);
                splitPlan(queryGraph, nextState, plans);
            });

        } else {
            // Just add every next state to the current plan
            for (State nextState : currentState.getNextSteps().values()) {
                splitPlan(queryGraph, nextState, plans);
            }
        }
    }

    private static void addPreviousStatesToPlan(final State currentState, final Set<State> plan) {
        plan.add(currentState);
        for (State previousState : currentState.getPreviousStates()) {
            addPreviousStatesToPlan(previousState, plan);
        }
    }
}

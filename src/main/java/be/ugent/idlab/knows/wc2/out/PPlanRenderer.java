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

        // Render each plan as a P-Plan
        Graph planGraph = GraphFactory.createDefaultGraph();
        int plan_nr = 1;
        for (List<State> plan : plans) {
            Node thePlan = NodeFactory.createURI(baseIRI + "pplan_" + plan_nr);
            planGraph.add(thePlan, a, ppClass);
            Node previousStep = null;
            for (State state : plan) {
                if (!state.getNextSteps().isEmpty()) {
                    String step = state.getNextSteps().entrySet().stream()
                            .filter(stepToState -> plan.contains(stepToState.getValue()))
                            .map(Map.Entry::getKey)
                            .toList().getFirst();
                    Node fnoStep =  NodeFactory.createURI(step);
                    String ppStepIRI =  step.substring(step.lastIndexOf('#') + 1);
                    Node stepNode = NodeFactory.createURI(baseIRI + ppStepIRI + '_' + plan_nr);
                    planGraph.add(stepNode, stepOfPlan, thePlan);
                    planGraph.add(stepNode, usesStep, fnoStep);
                    if (previousStep != null) {
                        planGraph.add(stepNode, isPrecededBy, previousStep);
                    }
                    previousStep = stepNode;
                }
            }
            plan_nr++;
        }
        StringWriter out = new StringWriter();
        RDFDataMgr.write(out, planGraph, RDFFormat.TURTLE_BLOCKS);
        return out.toString();
    }

    /**
     * Splits a plan into separate plans. This happens when there are exclusive OR paths in the plan: each such path
     * is a separate plan.
     * @param queryGraph The graph of states and steps
     * @return  A list of plans. Each plan is a list of States
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

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
    private final static Node hasCost = NodeFactory.createURI(fnoStepsPrefix + "cost");


    @Override
    public String render(final QueryGraph queryGraph) {
        // Split plan into separate plans.
        List<Set<State>> plans = splitPlan(queryGraph);

        // Render each plan as a P-Plan
        Graph planGraph = GraphFactory.createDefaultGraph();
        State currentState = queryGraph.getStartState();
        int plan_nr = 1;
        for (Set<State> plan : plans) {
            Node thePlan = NodeFactory.createURI(baseIRI + "pplan_" + plan_nr);
            // For now, the cost is just the number of steps involved in the plan
            int cost = renderPlan(planGraph, plan, plan_nr, thePlan, currentState, null);
            if (cost > 0) {
                planGraph.add(thePlan, a, ppClass);
                planGraph.add(thePlan, hasCost, NodeFactory.createLiteralByValue(cost));
                ++plan_nr;
            }
        }
        StringWriter out = new StringWriter();
        RDFDataMgr.write(out, planGraph, RDFFormat.TURTLE_BLOCKS);
        return out.toString();
    }

    private int renderPlan(final Graph planGraph,
                            final Set<State> plan,
                            final int planNr,
                            final Node thePlan,
                            final State currentState,
                            final Node previousStep) {

        // the steps starting from this state
         List<Map.Entry<String, State>> stepToStates = currentState.getNextSteps().entrySet().stream()
                .filter(stepToState -> plan.contains(stepToState.getValue()))
                .toList();

        int cost = 0;

        for (Map.Entry<String, State> stepToState : stepToStates) {

            String step = stepToState.getKey();
            Node fnoStep =  NodeFactory.createURI(step);
            String ppStepIRI =  step.substring(step.lastIndexOf('#') + 1);
            Node stepNode = NodeFactory.createURI(baseIRI + ppStepIRI + '_' + planNr);
            State nextState = stepToState.getValue();
            if (nextState.getStatus().equals(Status.Todo)) {
                planGraph.add(stepNode, stepOfPlan, thePlan);
                planGraph.add(stepNode, usesStep, fnoStep);
                if (currentState.getStatus().equals(Status.Todo) && previousStep != null) {
                    planGraph.add(stepNode, isPrecededBy, previousStep);
                }
                ++cost;
            }

            cost += renderPlan(planGraph, plan, planNr, thePlan, nextState, stepNode);
        }
        return cost;
    }



    /**
     * Splits a plan into separate plans. This happens when there are exclusive OR paths in the plan: each such path
     * is a separate plan.
     * @param queryGraph The graph of states and steps
     * @return  A list of plans. Each plan is a set of States
     */
    private static List<Set<State>> splitPlan(final QueryGraph queryGraph) {
        List<Set<State>> plans = new ArrayList<>(1);
        plans.add(new HashSet<>());
        splitPlan(queryGraph.getStartState(), plans);
        return plans;
    }

    private static void splitPlan(final State currentState, final List<Set<State>> plans) {
        // Don't include deleted states
        if (currentState.getStatus().equals(Status.Deleted)) {
            return;
        }

        // add current state to latest plan
        Set<State> currentPlan = plans.getLast();
        currentPlan.add(currentState);

        // If the operator is XOR, then a new plan will be created
        if (currentState.getOperator().equals(XOR)) {
            Iterator<State> nextStateIter = currentState.getNextSteps().values().iterator();

            // The first state of the next states can be added to the current plan
            State firstNextState = nextStateIter.next();
            splitPlan(firstNextState, plans);

            // The rest needs new plans
            nextStateIter.forEachRemaining(nextState -> {
                Set<State> newPlan = new HashSet<>();
                addPreviousStatesToPlan(nextState, newPlan);
                plans.add(newPlan);
                splitPlan(nextState, plans);
            });

        } else {
            // Just add every next state to the current plan
            for (State nextState : currentState.getNextSteps().values()) {
                splitPlan(nextState, plans);
            }
        }
    }

    private static void addPreviousStatesToPlan(final State currentState, final Set<State> plan) {
        for (State previousState : currentState.getPreviousStates()) {
            if (!previousState.getStatus().equals(Status.Done) && !previousState.getStatus().equals(Status.Deleted)) {
                plan.add(previousState);
                addPreviousStatesToPlan(previousState, plan);
            }
        }
    }
}

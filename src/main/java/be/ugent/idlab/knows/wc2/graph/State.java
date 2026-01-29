package be.ugent.idlab.knows.wc2.graph;

import java.util.*;

import static be.ugent.idlab.knows.wc2.graph.Operator.*;

public class State {
    private final String iri;
    private final Set<Step> nextSteps = new HashSet<>();
    private final Set<Step> previousSteps = new HashSet<>();
    private Operator operator = None;
    private boolean operatorFixed = false;

    public State(String iri) {
        this.iri = iri;
    }

    public void setPreviousStep(final Step step) {
        step.setNextState(this);
        previousSteps.add(step);
    }

    public void setNextStep(final Step step) {
        step.setPreviousState(this);
        nextSteps.add(step);
    }

    public void fixOperator() {
        fixOperator(None);
    }

    public void fixOperator(Operator operator) {
        if (!operatorFixed) {
            operatorFixed = true;
            if (nextSteps.size() > 1) {
                if (operator == AND) {
                    this.operator = AND;
                } else {
                    this.operator = OR;
                }
                operator = None;
            }

            if (previousSteps.size() > 1) {
                for (Step previousStep : previousSteps) {
                    previousStep.pushBackOperator(OR);
                }
            } else {
                for (Step previousStep : previousSteps) { // *should* be only one
                    if (previousStep.hasMultiplePreviousStates()) {
                        previousStep.pushBackOperator(AND);
                    } else {
                        //this.operator = operator;
                        //if (operator != None) {}
                        previousStep.pushBackOperator(operator);
                    }
                }
            }
        }
    }



/*        if (Objects.requireNonNull(operator) == None) {
            if (nextSteps.isEmpty()) {
                if (previousSteps.size() > 1) {
                    for (Step previousStep : previousSteps) {
                        previousStep.pushBackOperator(OR);
                    }
                } else {
                    Step previousStep = previousSteps.iterator().next();
                    if (previousStep.hasMultiplePreviousStates()) {
                        previousStep.pushBackOperator(AND);
                    }
                }
            }
        } else {
            if (nextSteps.size() > 1) {
                // This is a split, so apply the operator
                this.operator = operator;
                for (Step previousStep : previousSteps) {
                    previousStep.pushBackOperator(None);
                }
            } else {
                // This is a
                for (Step previousStep : previousSteps) {
                    previousStep.pushBackOperator(operator);
                }
            }
        }

    }*/

    /*void pushBackOperator(final Operator operator) {
        if (nextSteps.size() > 1) {
            this.operator = operator;
        } else {
            for (Step previousStep : previousSteps) {
                previousStep.pushBackOperator(operator);
            }
        }
    }*/

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return Objects.equals(iri, state.iri);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(iri);
    }
}

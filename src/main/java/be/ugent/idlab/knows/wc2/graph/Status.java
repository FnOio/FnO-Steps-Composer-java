package be.ugent.idlab.knows.wc2.graph;

public enum Status {
    Todo(0),
    Current(1),
    Done(2),
    Deleted(3);

    private final int rank;

    Status(int rank) {
        this.rank = rank;
    }

    public Status getHighestStatus(Status otherStatus) {
        if (this.rank >= otherStatus.rank) {
            return this;
        } else {
            return otherStatus;
        }
    }
}

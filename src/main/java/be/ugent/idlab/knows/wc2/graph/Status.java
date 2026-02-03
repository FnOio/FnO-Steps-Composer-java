package be.ugent.idlab.knows.wc2.graph;

public enum Status {
    None(0),
    Todo(1),
    Current(2),
    Done(3),
    Deleted(4);

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

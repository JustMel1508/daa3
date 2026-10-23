package algorithms;

/** componentOf[v] is the component number; component numbers have no semantic order. */
public record SccResult(int[] componentOf, int count) { }

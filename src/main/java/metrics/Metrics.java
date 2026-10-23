package metrics;

/** Counters are incremented at the operations, never reconstructed from formulas. */
public final class Metrics {
    public long dsuFinds;
    public long dsuUnions;
    public long edgeScans;
    public long comparisons;
    public int maxDepth;

    public long dsuOps() {
        return dsuFinds + dsuUnions;
    }

    public void observeDepth(int depth) {
        maxDepth = Math.max(maxDepth, depth);
    }
}

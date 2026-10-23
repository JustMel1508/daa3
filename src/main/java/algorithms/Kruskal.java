package algorithms;

import graph.Graph;
import java.util.ArrayList;
import java.util.List;
import metrics.Metrics;

public final class Kruskal {
    private Kruskal() { }

    public static MstResult run(Graph graph, Metrics metrics) {
        if (graph.directed()) throw new IllegalArgumentException("MST requires an undirected graph");
        List<Graph.Edge> sorted = new ArrayList<>(graph.edges());
        sorted.sort((left, right) -> {
            metrics.comparisons++;
            return Integer.compare(left.weight(), right.weight());
        });
        DSU dsu = new DSU(graph.n(), metrics);
        List<Graph.Edge> forest = new ArrayList<>();
        long weight = 0;
        for (Graph.Edge edge : sorted) {
            if (dsu.components() <= 1) break;
            metrics.edgeScans++;
            // union checks the two roots with find and only joins different sets.
            if (dsu.union(edge.from(), edge.to())) {
                forest.add(edge);
                weight += edge.weight();
            }
        }
        return new MstResult(forest, weight, dsu.components());
    }
}

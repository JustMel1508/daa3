package algorithms;

import graph.Graph;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import metrics.Metrics;

/** Lazy Prim: a queue of candidate edges, restarted for each disconnected component. */
public final class Prim {
    private Prim() { }

    public static MstResult run(Graph graph, Metrics metrics) {
        if (graph.directed()) throw new IllegalArgumentException("MST requires an undirected graph");
        boolean[] inTree = new boolean[graph.n()];
        PriorityQueue<Graph.Edge> queue = new PriorityQueue<>((left, right) -> {
            metrics.comparisons++;
            int byWeight = Integer.compare(left.weight(), right.weight());
            if (byWeight != 0) return byWeight;
            int byFrom = Integer.compare(left.from(), right.from());
            return byFrom != 0 ? byFrom : Integer.compare(left.to(), right.to());
        });
        List<Graph.Edge> forest = new ArrayList<>();
        long totalWeight = 0;
        int components = 0;
        // Start with vertex 0, then cover components not reachable from it.
        for (int root = 0; root < graph.n(); root++) {
            if (inTree[root]) continue;
            components++;
            addVertex(graph, root, inTree, queue, metrics);
            while (!queue.isEmpty()) {
                Graph.Edge lightest = queue.remove();
                if (inTree[lightest.to()]) continue;
                forest.add(lightest);
                totalWeight += lightest.weight();
                addVertex(graph, lightest.to(), inTree, queue, metrics);
            }
        }
        return new MstResult(forest, totalWeight, components);
    }

    private static void addVertex(Graph graph, int vertex, boolean[] inTree,
                                  PriorityQueue<Graph.Edge> queue, Metrics metrics) {
        inTree[vertex] = true;
        for (Graph.Edge edge : graph.adj(vertex)) {
            // Count adjacency entries examined; an undirected edge has two entries.
            metrics.edgeScans++;
            if (!inTree[edge.to()]) queue.add(edge);
        }
    }
}

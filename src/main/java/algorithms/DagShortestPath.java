package algorithms;

import graph.Graph;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import metrics.Metrics;

/** Multi-source shortest paths: every original source has a zero-cost super-source edge. */
public final class DagShortestPath {
    private static final long INF = Long.MAX_VALUE / 4;

    private DagShortestPath() { }

    public record Result(long[] distance, int[] parent) {
        public long sumDistances() {
            long sum = 0;
            for (long value : distance) sum += value;
            return sum;
        }

        public List<Integer> pathTo(int target) {
            if (target < 0 || target >= parent.length) throw new IllegalArgumentException("Invalid target");
            if (distance[target] == INF) return List.of();
            List<Integer> path = new ArrayList<>();
            for (int vertex = target; vertex != -1; vertex = parent[vertex]) path.add(vertex);
            Collections.reverse(path);
            return path;
        }
    }

    public static Result run(Graph graph, Metrics metrics) {
        // Preparation is timed, but its scans belong to Kahn, not to the relaxation counter.
        TopologicalResult topological = Kahn.run(graph, new Metrics());
        if (topological.hasCycle()) throw new IllegalArgumentException("Shortest-path graph contains a cycle");
        return relax(graph, topological.order(), metrics);
    }

    public static Result run(Graph graph, List<Integer> order, Metrics metrics) {
        if (!Kahn.isValid(graph, order)) throw new IllegalArgumentException("Invalid topological order");
        return relax(graph, order, metrics);
    }

    private static Result relax(Graph graph, List<Integer> order, Metrics metrics) {
        long[] distance = new long[graph.n()];
        Arrays.fill(distance, INF);
        int[] parent = new int[graph.n()];
        Arrays.fill(parent, -1);
        int[] indegree = graph.indegrees();
        for (int vertex = 0; vertex < graph.n(); vertex++) {
            if (indegree[vertex] == 0) distance[vertex] = 0;
        }
        for (int vertex : order) {
            for (Graph.Edge edge : graph.adj(vertex)) {
                metrics.edgeScans++;
                if (distance[vertex] == INF) continue;
                long candidate = distance[vertex] + edge.weight();
                // This counter includes comparisons of candidate path weights.
                metrics.comparisons++;
                if (candidate < distance[edge.to()]) {
                    distance[edge.to()] = candidate;
                    parent[edge.to()] = vertex;
                }
            }
        }
        return new Result(distance, parent);
    }
}

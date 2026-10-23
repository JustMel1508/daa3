package algorithms;

import graph.Graph;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import metrics.Metrics;

public final class Kahn {
    private Kahn() { }

    public static TopologicalResult run(Graph graph, Metrics metrics) {
        if (!graph.directed()) throw new IllegalArgumentException("Topological order requires a directed graph");
        // Degree construction is preprocessing; scans count edges removed by Kahn's loop.
        int[] indegree = graph.indegrees();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int vertex = 0; vertex < graph.n(); vertex++) {
            if (indegree[vertex] == 0) queue.addLast(vertex);
        }
        List<Integer> order = new ArrayList<>(graph.n());
        while (!queue.isEmpty()) {
            int vertex = queue.removeFirst();
            order.add(vertex);
            for (Graph.Edge edge : graph.adj(vertex)) {
                metrics.edgeScans++;
                if (--indegree[edge.to()] == 0) queue.addLast(edge.to());
            }
        }
        if (order.size() != graph.n()) return new TopologicalResult(List.of(), true);
        return new TopologicalResult(order, false);
    }

    public static boolean isValid(Graph graph, List<Integer> order) {
        if (!graph.directed() || order == null || order.size() != graph.n()) return false;
        int[] position = new int[graph.n()];
        boolean[] seen = new boolean[graph.n()];
        for (int index = 0; index < order.size(); index++) {
            Integer vertex = order.get(index);
            if (vertex == null || vertex < 0 || vertex >= graph.n() || seen[vertex]) return false;
            seen[vertex] = true;
            position[vertex] = index;
        }
        for (Graph.Edge edge : graph.edges()) {
            if (position[edge.from()] >= position[edge.to()]) return false;
        }
        return true;
    }
}

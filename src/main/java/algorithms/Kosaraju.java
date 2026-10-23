package algorithms;

import graph.Graph;
import java.util.Arrays;
import java.util.List;
import metrics.Metrics;

/** Both DFS passes use explicit frames, so a 100,000-vertex path uses no recursion. */
public final class Kosaraju {
    private Kosaraju() { }

    public static SccResult run(Graph graph, Metrics metrics) {
        if (!graph.directed()) throw new IllegalArgumentException("SCC requires a directed graph");
        int n = graph.n();
        boolean[] visited = new boolean[n];
        int[] finish = new int[n];
        int finishSize = 0;
        int[] stack = new int[n];
        int[] nextEdge = new int[n];

        for (int start = 0; start < n; start++) {
            if (visited[start]) continue;
            int depth = 1;
            stack[0] = start;
            nextEdge[0] = 0;
            visited[start] = true;
            metrics.observeDepth(depth);
            while (depth > 0) {
                int frame = depth - 1;
                int vertex = stack[frame];
                List<Graph.Edge> outgoing = graph.adj(vertex);
                if (nextEdge[frame] == outgoing.size()) {
                    finish[finishSize++] = vertex;
                    depth--;
                    continue;
                }
                Graph.Edge edge = outgoing.get(nextEdge[frame]++);
                metrics.edgeScans++;
                if (!visited[edge.to()]) {
                    visited[edge.to()] = true;
                    stack[depth] = edge.to();
                    nextEdge[depth] = 0;
                    depth++;
                    metrics.observeDepth(depth);
                }
            }
        }

        // Transpose construction is preprocessing. edgeScans measures the two DFS passes.
        Graph reversed = graph.reverse();
        int[] componentOf = new int[n];
        Arrays.fill(componentOf, -1);
        int components = 0;
        for (int index = finishSize - 1; index >= 0; index--) {
            int start = finish[index];
            if (componentOf[start] != -1) continue;
            int depth = 1;
            stack[0] = start;
            nextEdge[0] = 0;
            componentOf[start] = components;
            metrics.observeDepth(depth);
            while (depth > 0) {
                int frame = depth - 1;
                int vertex = stack[frame];
                List<Graph.Edge> outgoing = reversed.adj(vertex);
                if (nextEdge[frame] == outgoing.size()) {
                    depth--;
                    continue;
                }
                Graph.Edge edge = outgoing.get(nextEdge[frame]++);
                metrics.edgeScans++;
                if (componentOf[edge.to()] == -1) {
                    componentOf[edge.to()] = components;
                    stack[depth] = edge.to();
                    nextEdge[depth] = 0;
                    depth++;
                    metrics.observeDepth(depth);
                }
            }
            components++;
        }
        return new SccResult(componentOf, components);
    }
}

package algorithms;

import graph.Graph;
import java.util.List;

public record MstResult(List<Graph.Edge> edges, long totalWeight, int components) {
    public MstResult {
        edges = List.copyOf(edges);
    }
}

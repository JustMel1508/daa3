package algorithms;

import java.util.List;

public record TopologicalResult(List<Integer> order, boolean hasCycle) {
    public TopologicalResult {
        order = List.copyOf(order);
    }
}

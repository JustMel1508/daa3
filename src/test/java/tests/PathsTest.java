package tests;

import algorithms.DagShortestPath;
import algorithms.Kahn;
import algorithms.TopologicalResult;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class PathsTest {
    @Test
    void kahnProducesAnOrderThatRespectsEveryEdge() {
        Graph graph = new Graph(7, true);
        graph.addEdge(0, 2, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(1, 4, 1);
        graph.addEdge(4, 5, 1);
        Metrics metrics = new Metrics();
        TopologicalResult result = Kahn.run(graph, metrics);
        assertFalse(result.hasCycle());
        assertEquals(graph.n(), result.order().size());
        assertTrue(Kahn.isValid(graph, result.order()));
        assertEquals(graph.edges().size(), metrics.edgeScans);
        assertFalse(Kahn.isValid(graph, List.of(0, 1, 2, 3, 4, 5, 5)));
        assertFalse(Kahn.isValid(graph, List.of(6, 5, 4, 3, 2, 1, 0)));
    }

    @Test
    void cyclicInputIsReportedAndRejectedByDagShortestPaths() throws Exception {
        Graph graph = Graph.read(Path.of("data", "cyclic.txt"));
        TopologicalResult result = Kahn.run(graph, new Metrics());
        assertTrue(result.hasCycle());
        assertTrue(result.order().size() < graph.n());
        assertFalse(Kahn.isValid(graph, result.order()));
        assertThrows(IllegalArgumentException.class,
                () -> DagShortestPath.run(graph, new Metrics()));
    }

    @Test
    void singleVertexSelfLoopIsACycle() {
        Graph graph = new Graph(1, true);
        graph.addEdge(0, 0, 1);
        assertTrue(Kahn.run(graph, new Metrics()).hasCycle());
    }

    @Test
    void randomNegativeWeightDagsAgreeWithBellmanFordFromAllSources() {
        Random random = new Random(8675309);
        for (int trial = 0; trial < 100; trial++) {
            int n = 2 + random.nextInt(39);
            Graph graph = new Graph(n, true);
            int[] rank = new int[n];
            for (int i = 0; i < n; i++) rank[i] = i;
            for (int i = n - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int value = rank[i]; rank[i] = rank[j]; rank[j] = value;
            }
            int m = random.nextInt(5 * n + 1);
            for (int i = 0; i < m; i++) {
                int a = random.nextInt(n);
                int b;
                do { b = random.nextInt(n); } while (a == b);
                if (rank[a] > rank[b]) { int value = a; a = b; b = value; }
                graph.addEdge(a, b, random.nextInt(41) - 20);
            }
            assertShortestPaths(graph);
        }
    }

    @Test
    void multipleSourcesAndIsolatedVerticesStartAtZero() {
        Graph graph = new Graph(6, true);
        graph.addEdge(0, 2, 8);
        graph.addEdge(1, 2, -3);
        graph.addEdge(2, 3, 2);
        graph.addEdge(1, 4, 7);
        DagShortestPath.Result result = DagShortestPath.run(graph, new Metrics());
        assertArrayEquals(new long[]{0, 0, -3, -1, 7, 0}, result.distance());
        assertEquals(List.of(1, 2, 3), result.pathTo(3));
        assertEquals(List.of(5), result.pathTo(5));
        assertEquals(3, result.sumDistances());
    }

    @Test
    void negativeWeightInputGraphIsTestedAndPathsAreRestored() throws Exception {
        assertShortestPaths(Graph.read(Path.of("data", "dag_negative.txt")));
    }

    private static void assertShortestPaths(Graph graph) {
        Metrics metrics = new Metrics();
        DagShortestPath.Result result = DagShortestPath.run(graph, metrics);
        assertArrayEquals(bellmanFord(graph), result.distance());
        assertEquals(graph.edges().size(), metrics.edgeScans);
        assertEquals(Arrays.stream(result.distance()).sum(), result.sumDistances());
        int[] indegree = graph.indegrees();
        for (int target = 0; target < graph.n(); target++) {
            List<Integer> path = result.pathTo(target);
            assertFalse(path.isEmpty());
            assertEquals(target, path.get(path.size() - 1).intValue());
            assertEquals(0, indegree[path.get(0)]);
            assertEquals(-1, result.parent()[path.get(0)]);
            assertTrue(path.size() <= graph.n());
            long weight = 0;
            for (int i = 1; i < path.size(); i++) {
                int from = path.get(i - 1);
                int to = path.get(i);
                assertEquals(from, result.parent()[to]);
                long difference = result.distance()[to] - result.distance()[from];
                assertTrue(graph.adj(from).stream().anyMatch(edge ->
                        edge.to() == to && edge.weight() == difference), "missing parent edge");
                weight += difference;
            }
            assertEquals(result.distance()[target], weight);
        }
    }

    private static long[] bellmanFord(Graph graph) {
        long infinity = Long.MAX_VALUE / 4;
        long[] distance = new long[graph.n()];
        Arrays.fill(distance, infinity);
        int[] indegree = graph.indegrees();
        for (int vertex = 0; vertex < graph.n(); vertex++) {
            if (indegree[vertex] == 0) distance[vertex] = 0;
        }
        for (int pass = 1; pass < graph.n(); pass++) {
            boolean changed = false;
            for (Graph.Edge edge : graph.edges()) {
                if (distance[edge.from()] == infinity) continue;
                long candidate = distance[edge.from()] + edge.weight();
                if (candidate < distance[edge.to()]) {
                    distance[edge.to()] = candidate;
                    changed = true;
                }
            }
            if (!changed) break;
        }
        return distance;
    }
}

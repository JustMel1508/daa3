package tests;

import algorithms.Kosaraju;
import algorithms.SccResult;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SCCTest {
    @Test
    void randomSccPartitionsAgreeWithBruteForceReachability() {
        Random random = new Random(2026);
        for (int trial = 0; trial < 31; trial++) {
            int n = trial == 30 ? 200 : 1 + random.nextInt(65);
            Graph graph = new Graph(n, true);
            int edges = random.nextInt(6 * n + 1);
            for (int i = 0; i < edges; i++) {
                graph.addEdge(random.nextInt(n), random.nextInt(n), 1);
            }
            assertBruteForcePartition(graph);
        }
    }

    @Test
    void cyclicInputGraphIsAlsoCheckedAgainstBruteForce() throws Exception {
        assertBruteForcePartition(Graph.read(Path.of("data", "cyclic.txt")));
    }

    @Test
    void explicitStackSurvivesAPathOfOneHundredThousandVertices() {
        int n = 100_000;
        Graph path = new Graph(n, true);
        for (int i = 0; i + 1 < n; i++) path.addEdge(i, i + 1, 1);
        Metrics metrics = new Metrics();
        SccResult result = assertDoesNotThrow(() -> Kosaraju.run(path, metrics));
        assertEquals(n, result.count());
        assertEquals(n, Arrays.stream(result.componentOf()).distinct().count());
        assertEquals(2L * (n - 1), metrics.edgeScans);
        assertEquals(n, metrics.maxDepth);
    }

    @Test
    void isolatedVerticesAndSelfLoopHaveDistinctComponents() {
        Graph graph = new Graph(4, true);
        graph.addEdge(1, 1, 5);
        assertBruteForcePartition(graph);
        assertEquals(4, Kosaraju.run(graph, new Metrics()).count());
    }

    private static void assertBruteForcePartition(Graph graph) {
        Metrics metrics = new Metrics();
        SccResult actual = Kosaraju.run(graph, metrics);
        boolean[][] reachable = new boolean[graph.n()][graph.n()];
        for (int start = 0; start < graph.n(); start++) {
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            reachable[start][start] = true;
            queue.add(start);
            while (!queue.isEmpty()) {
                int vertex = queue.remove();
                for (Graph.Edge edge : graph.adj(vertex)) {
                    if (!reachable[start][edge.to()]) {
                        reachable[start][edge.to()] = true;
                        queue.add(edge.to());
                    }
                }
            }
        }
        for (int u = 0; u < graph.n(); u++) {
            for (int v = 0; v < graph.n(); v++) {
                boolean mutuallyReachable = reachable[u][v] && reachable[v][u];
                assertEquals(mutuallyReachable, actual.componentOf()[u] == actual.componentOf()[v],
                        "vertices " + u + " and " + v);
            }
        }
        assertEquals(actual.count(), Arrays.stream(actual.componentOf()).distinct().count());
        assertEquals(2L * graph.edges().size(), metrics.edgeScans);
    }
}

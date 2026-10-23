package tests;

import algorithms.Kruskal;
import algorithms.MstResult;
import algorithms.Prim;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MSTTest {
    @Test
    void kruskalAndPrimAgreeOnOneHundredRandomConnectedGraphs() {
        Random random = new Random(314159);
        for (int trial = 0; trial < 100; trial++) {
            int n = 2 + random.nextInt(59);
            Graph graph = new Graph(n, false);
            for (int vertex = 1; vertex < n; vertex++) {
                graph.addEdge(random.nextInt(vertex), vertex, random.nextInt(101) - 20);
            }
            for (int i = 0; i < 4 * n; i++) {
                graph.addEdge(random.nextInt(n), random.nextInt(n), random.nextInt(101) - 20);
            }
            MstResult kruskal = Kruskal.run(graph, new Metrics());
            MstResult prim = Prim.run(graph, new Metrics());
            assertEquals(kruskal.totalWeight(), prim.totalWeight(), "trial " + trial);
            assertEquals(n - 1, kruskal.edges().size());
            assertEquals(n - 1, prim.edges().size());
            assertEquals(1, kruskal.components());
            assertEquals(1, prim.components());
            assertForest(n, kruskal);
            assertForest(n, prim);
        }
    }

    @Test
    void disconnectedGraphWithParallelEdgesSelfLoopsAndIsolatedVertexReturnsForest() {
        Graph graph = new Graph(6, false);
        graph.addEdge(0, 1, 8);
        graph.addEdge(0, 1, 3);
        graph.addEdge(1, 2, -2);
        graph.addEdge(0, 2, 6);
        graph.addEdge(3, 4, 5);
        graph.addEdge(4, 4, -10);
        for (MstResult result : new MstResult[]{
                Kruskal.run(graph, new Metrics()), Prim.run(graph, new Metrics())}) {
            assertEquals(3, result.components());
            assertEquals(3, result.edges().size());
            assertEquals(6, result.totalWeight());
            assertForest(graph.n(), result);
        }
    }

    @Test
    void inputFilesAreUsedForBothMstAlgorithms() throws Exception {
        for (String file : new String[]{"connected.txt", "disconnected.txt"}) {
            Graph graph = Graph.read(Path.of("data", file));
            MstResult first = Kruskal.run(graph, new Metrics());
            MstResult second = Prim.run(graph, new Metrics());
            assertEquals(first.totalWeight(), second.totalWeight());
            assertEquals(first.components(), second.components());
            assertEquals(graph.n() - first.components(), first.edges().size());
            if (file.equals("connected.txt")) assertEquals(1, first.components());
            else assertTrue(first.components() > 1);
        }
    }

    @Test
    void singleVertexSelfLoopIsNotIncludedInMst() {
        Graph graph = new Graph(1, false);
        graph.addEdge(0, 0, -100);
        for (MstResult result : new MstResult[]{
                Kruskal.run(graph, new Metrics()), Prim.run(graph, new Metrics())}) {
            assertEquals(1, result.components());
            assertEquals(0, result.totalWeight());
            assertTrue(result.edges().isEmpty());
        }
    }

    private static void assertForest(int n, MstResult result) {
        int[] labels = new int[n];
        for (int i = 0; i < n; i++) labels[i] = i;
        long total = 0;
        for (Graph.Edge edge : result.edges()) {
            assertNotEquals(labels[edge.from()], labels[edge.to()], "cycle in returned forest");
            int oldLabel = labels[edge.to()];
            int newLabel = labels[edge.from()];
            for (int i = 0; i < n; i++) {
                if (labels[i] == oldLabel) labels[i] = newLabel;
            }
            total += edge.weight();
        }
        assertEquals(result.totalWeight(), total);
        assertEquals(n - result.components(), result.edges().size());
    }
}

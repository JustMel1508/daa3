package tests;

import algorithms.DagShortestPath;
import algorithms.Kahn;
import algorithms.Kosaraju;
import algorithms.Kruskal;
import algorithms.Prim;
import benchmark.GraphGenerator;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReferenceTest {
    @Test
    void exactSeedGeneratorReproducesTheFirstReferenceRow() {
        int n = 1000;
        int m = 4000;
        Graph undirected = GraphGenerator.undirected(n, m);
        assertEquals(m, undirected.edges().size());
        assertEquals(147970L, Kruskal.run(undirected, new Metrics()).totalWeight());
        assertEquals(147970L, Prim.run(undirected, new Metrics()).totalWeight());

        Graph directed = GraphGenerator.directed(n, m);
        Metrics sccMetrics = new Metrics();
        assertEquals(27, Kosaraju.run(directed, sccMetrics).count());
        assertEquals(2L * m, sccMetrics.edgeScans);

        Graph dag = GraphGenerator.dag(n, m);
        Metrics kahnMetrics = new Metrics();
        var order = Kahn.run(dag, kahnMetrics);
        assertFalse(order.hasCycle());
        assertTrue(Kahn.isValid(dag, order.order()));
        assertEquals(m, kahnMetrics.edgeScans);
        Metrics pathMetrics = new Metrics();
        assertEquals(396274L, DagShortestPath.run(dag, pathMetrics).sumDistances());
        assertEquals(m, pathMetrics.edgeScans);
    }
}

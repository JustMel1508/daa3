package tests;

import graph.Graph;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GraphTest {
    @Test
    void oneRepresentationSupportsDirectedAndUndirectedEdges() {
        Graph directed = new Graph(3, true);
        directed.addEdge(0, 1, 7);
        directed.addEdge(0, 1, 2);
        directed.addEdge(1, 1, -3);
        assertTrue(directed.directed());
        assertEquals(3, directed.edges().size());
        assertEquals(2, directed.adj(0).size());
        assertTrue(directed.adj(2).isEmpty());
        assertArrayEquals(new int[]{0, 3, 0}, directed.indegrees());

        Graph undirected = new Graph(3, false);
        undirected.addEdge(0, 1, 7);
        assertFalse(undirected.directed());
        assertEquals(1, undirected.edges().size());
        assertEquals(1, undirected.adj(0).get(0).to());
        assertEquals(0, undirected.adj(1).get(0).to());
        assertTrue(undirected.adj(2).isEmpty());
    }

    @Test
    void reversalPreservesWeightsAndParallelEdges() {
        Graph graph = new Graph(4, true);
        graph.addEdge(0, 1, 9);
        graph.addEdge(0, 1, -2);
        graph.addEdge(1, 2, 4);
        graph.addEdge(2, 2, 5);
        Graph reversed = graph.reverse();
        assertEquals(graph.n(), reversed.n());
        assertEquals(graph.edges().size(), reversed.edges().size());
        for (Graph.Edge edge : graph.edges()) {
            assertTrue(reversed.edges().contains(
                    new Graph.Edge(edge.to(), edge.from(), edge.weight())));
        }
        assertTrue(reversed.adj(3).isEmpty());
    }

    @Test
    void allProvidedInputGraphsCanBeRead() throws Exception {
        Graph connected = Graph.read(Path.of("data", "connected.txt"));
        Graph disconnected = Graph.read(Path.of("data", "disconnected.txt"));
        Graph cyclic = Graph.read(Path.of("data", "cyclic.txt"));
        Graph negativeDag = Graph.read(Path.of("data", "dag_negative.txt"));
        assertEquals(5, connected.n());
        assertFalse(connected.directed());
        assertEquals(6, disconnected.n());
        assertFalse(disconnected.directed());
        assertEquals(5, cyclic.n());
        assertTrue(cyclic.directed());
        assertTrue(negativeDag.directed());
        assertTrue(negativeDag.edges().stream().anyMatch(edge -> edge.weight() < 0));
    }
}

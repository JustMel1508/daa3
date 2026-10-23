package graph;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A weighted adjacency-list graph. edges() stores each input edge exactly once. */
public final class Graph {
    public record Edge(int from, int to, int weight) { }

    private final int vertexCount;
    private final boolean directed;
    private final List<Edge> edges = new ArrayList<>();
    private final List<List<Edge>> adjacency;
    private final List<List<Edge>> adjacencyViews;

    public Graph(int n, boolean directed) {
        if (n < 0) throw new IllegalArgumentException("Vertex count must be nonnegative");
        vertexCount = n;
        this.directed = directed;
        adjacency = new ArrayList<>(n);
        adjacencyViews = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            List<Edge> list = new ArrayList<>();
            adjacency.add(list);
            adjacencyViews.add(Collections.unmodifiableList(list));
        }
    }

    public int n() { return vertexCount; }
    public boolean directed() { return directed; }
    public List<Edge> edges() { return Collections.unmodifiableList(edges); }

    public List<Edge> adj(int vertex) {
        checkVertex(vertex);
        return adjacencyViews.get(vertex);
    }

    public void addEdge(int from, int to, int weight) {
        checkVertex(from);
        checkVertex(to);
        Edge edge = new Edge(from, to, weight);
        edges.add(edge);
        adjacency.get(from).add(edge);
        if (!directed) adjacency.get(to).add(new Edge(to, from, weight));
    }

    public Graph reverse() {
        Graph reversed = new Graph(n(), directed);
        for (Edge edge : edges) {
            if (directed) reversed.addEdge(edge.to(), edge.from(), edge.weight());
            else reversed.addEdge(edge.from(), edge.to(), edge.weight());
        }
        return reversed;
    }

    public int[] indegrees() {
        int[] degree = new int[n()];
        for (List<Edge> list : adjacency) {
            for (Edge edge : list) degree[edge.to()]++;
        }
        return degree;
    }

    public static Graph read(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String header = nextDataLine(reader);
            if (header == null) throw new IOException("Graph file is empty: " + path);
            // Also accept the UTF-8 BOM sometimes added by Windows editors.
            if (header.startsWith("\uFEFF")) header = header.substring(1);
            String[] parts = header.trim().split("\\s+");
            if (parts.length != 3) throw new IOException("Expected: n m directed|undirected");
            int n = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            if (n < 0 || m < 0) throw new IOException("Negative graph size");
            boolean directed;
            if (parts[2].equals("directed")) directed = true;
            else if (parts[2].equals("undirected")) directed = false;
            else throw new IOException("Unknown graph type: " + parts[2]);
            Graph graph = new Graph(n, directed);
            for (int i = 0; i < m; i++) {
                String line = nextDataLine(reader);
                if (line == null) throw new IOException("Expected " + m + " edges, found " + i);
                String[] values = line.split("\\s+");
                if (values.length != 3) throw new IOException("Expected edge: u v w");
                graph.addEdge(Integer.parseInt(values[0]), Integer.parseInt(values[1]),
                        Integer.parseInt(values[2]));
            }
            if (nextDataLine(reader) != null) throw new IOException("Extra data after " + m + " edges");
            return graph;
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid graph file: " + path + ": " + exception.getMessage(), exception);
        }
    }

    private static String nextDataLine(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) return line;
        }
        return null;
    }

    private void checkVertex(int vertex) {
        if (vertex < 0 || vertex >= n()) {
            throw new IllegalArgumentException("Vertex " + vertex + " is outside 0.." + (n() - 1));
        }
    }
}

package benchmark;

import graph.Graph;
import java.util.Random;

/** Exact, deterministic generators from the assignment; a new seed for every graph. */
public final class GraphGenerator {
    private GraphGenerator() { }

    public static Graph undirected(int n, int m) {
        check(n, m);
        if (m < n - 1) throw new IllegalArgumentException("Connected graph needs at least n - 1 edges");
        Random random = new Random(42);
        int[] permutation = shuffled(n, random);
        Graph graph = new Graph(n, false);
        for (int i = 1; i < n; i++) {
            int parent = permutation[random.nextInt(i)];
            graph.addEdge(parent, permutation[i], 1 + random.nextInt(1000));
        }
        addRandomEdges(graph, m, random, null);
        return graph;
    }

    public static Graph directed(int n, int m) {
        check(n, m);
        Graph graph = new Graph(n, true);
        addRandomEdges(graph, m, new Random(42), null);
        return graph;
    }

    public static Graph dag(int n, int m) {
        check(n, m);
        Random random = new Random(42);
        int[] rank = shuffled(n, random);
        Graph graph = new Graph(n, true);
        addRandomEdges(graph, m, random, rank);
        return graph;
    }

    private static void addRandomEdges(Graph graph, int m, Random random, int[] rank) {
        for (int edgeIndex = graph.edges().size(); edgeIndex < m; edgeIndex++) {
            int a = random.nextInt(graph.n());
            int b = random.nextInt(graph.n());
            while (a == b) b = random.nextInt(graph.n());
            if (rank != null && rank[a] > rank[b]) {
                int temporary = a;
                a = b;
                b = temporary;
            }
            graph.addEdge(a, b, 1 + random.nextInt(1000));
        }
    }

    private static int[] shuffled(int n, Random random) {
        int[] values = new int[n];
        for (int i = 0; i < n; i++) values[i] = i;
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temporary = values[i];
            values[i] = values[j];
            values[j] = temporary;
        }
        return values;
    }

    private static void check(int n, int m) {
        if (n < 0 || m < 0 || (n < 2 && m != 0)) {
            throw new IllegalArgumentException("Invalid size for loop-free random graph");
        }
    }
}

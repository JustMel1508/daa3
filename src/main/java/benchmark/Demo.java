package benchmark;

import algorithms.DagShortestPath;
import algorithms.Kahn;
import algorithms.Kosaraju;
import algorithms.Kruskal;
import algorithms.Prim;
import graph.Graph;
import metrics.Metrics;
import java.nio.file.Path;
import java.util.Arrays;

/** Run a supplied text graph, or the three included examples when no path is supplied. */
public final class Demo {
    private Demo() { }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            for (String argument : args) show(Path.of(argument));
        } else {
            show(Path.of("data", "disconnected.txt"));
            show(Path.of("data", "cyclic.txt"));
            show(Path.of("data", "dag_negative.txt"));
        }
    }

    private static void show(Path file) throws Exception {
        Graph graph = Graph.read(file);
        System.out.println("\nGraph: " + file + "; n=" + graph.n() + "; m=" + graph.edges().size());
        if (!graph.directed()) {
            var kruskal = Kruskal.run(graph, new Metrics());
            var prim = Prim.run(graph, new Metrics());
            System.out.println("Kruskal forest: weight=" + kruskal.totalWeight()
                + ", components=" + kruskal.components() + ", edges=" + kruskal.edges());
            System.out.println("Prim forest: weight=" + prim.totalWeight()
                + ", components=" + prim.components());
            if (kruskal.totalWeight() != prim.totalWeight()) throw new IllegalStateException("MST disagreement");
        } else {
            var scc = Kosaraju.run(graph, new Metrics());
            System.out.println("SCC count=" + scc.count() + "; labels=" + Arrays.toString(scc.componentOf()));
            var topological = Kahn.run(graph, new Metrics());
            if (topological.hasCycle()) {
                System.out.println("Cycle detected: no topological order or DAG shortest paths.");
            } else {
                System.out.println("Topological order: " + topological.order());
                var paths = DagShortestPath.run(graph, new Metrics());
                System.out.println("Distances from all sources: " + Arrays.toString(paths.distance()));
                for (int vertex = 0; vertex < graph.n(); vertex++) {
                    System.out.println("Path to " + vertex + ": " + paths.pathTo(vertex));
                }
            }
        }
    }
}

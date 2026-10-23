package benchmark;

import algorithms.DagShortestPath;
import algorithms.Kahn;
import algorithms.Kosaraju;
import algorithms.Kruskal;
import algorithms.MstResult;
import algorithms.Prim;
import algorithms.TopologicalResult;
import graph.Graph;
import metrics.Metrics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Five timed runs per algorithm. Every row is checked before the CSV is replaced. */
public final class Benchmark {
    private static final int[] SIZES = {1000, 3162, 10000, 31623, 100000};
    private static final int[] DENSITIES = {4, 20};
    // Expected values are only assertions: algorithms compute every recorded result.
    private static final long[][] REFERENCE_MST = {
        {147970, 30363}, {468911, 97589}, {1486139, 309550},
        {4715515, 968517}, {15012094, 3056681}
    };
    private static final long[][] REFERENCE_SCC = {
        {27, 1}, {137, 1}, {373, 1}, {1188, 1}, {3923, 1}
    };
    private static final long[][] REFERENCE_DAG = {
        {396274, 252411}, {1271911, 723157}, {3941828, 2381342},
        {12551891, 7376612}, {40096539, 23512087}
    };
    private static final String HEADER =
        "algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result";
    private static final int RUNS = 5;

    private Benchmark() { }

    private record Measurement<T>(T result, Metrics metrics, double medianMilliseconds) { }

    public static void main(String[] args) throws IOException {
        boolean quick = args.length == 1 && args[0].equals("--quick");
        if (args.length > 0 && !quick) {
            throw new IllegalArgumentException("Usage: Benchmark [--quick]");
        }
        List<String> rows = new ArrayList<>();
        rows.add(HEADER);
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        System.out.println("Runs per algorithm: " + RUNS + "; time statistic: median");
        System.out.println("Mode: " + (quick ? "QUICK (only n=1000, results/quick.csv)" : "FULL (50 rows, results/results.csv)"));
        for (int sizeIndex = 0; sizeIndex < (quick ? 1 : SIZES.length); sizeIndex++) {
            for (int densityIndex = 0; densityIndex < DENSITIES.length; densityIndex++) {
                int n = SIZES[sizeIndex];
                int m = n * DENSITIES[densityIndex];
                String density = densityIndex == 0 ? "sparse" : "dense";
                long mstWeight = measureMst(n, m, density, REFERENCE_MST[sizeIndex][densityIndex], rows);
                // Reclaim the previous graph outside any timed interval.
                System.gc();
                long sccCount = measureScc(n, m, density, REFERENCE_SCC[sizeIndex][densityIndex], rows);
                System.gc();
                long distanceSum = measureDag(n, m, density, REFERENCE_DAG[sizeIndex][densityIndex], rows);
                System.out.printf(Locale.ROOT,
                    "OK n=%d m=%d: MST=%d SCC=%d DAG-sum=%d%n", n, m, mstWeight, sccCount, distanceSum);
                System.gc();
            }
        }
        int expectedRows = quick ? 10 : 50;
        require(rows.size() - 1 == expectedRows, "Incorrect CSV row count");
        Path destination = Path.of("results", quick ? "quick.csv" : "results.csv");
        Files.createDirectories(destination.getParent());
        Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");
        Files.write(temporary, rows, StandardCharsets.UTF_8);
        Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("PASS: all reference values, MST agreement, orders and counters checked.");
        System.out.println("Saved " + expectedRows + " data rows to " + destination);
    }

    private static long measureMst(int n, int m, String density, long reference, List<String> rows) {
        Graph graph = GraphGenerator.undirected(n, m);
        Measurement<MstResult> kruskal = measure(metrics -> Kruskal.run(graph, metrics));
        Measurement<MstResult> prim = measure(metrics -> Prim.run(graph, metrics));
        require(kruskal.result.totalWeight() == prim.result.totalWeight(), "Kruskal and Prim disagree");
        for (MstResult result : List.of(kruskal.result, prim.result)) {
            require(result.totalWeight() == reference, "MST reference mismatch: " + n + "/" + m
                + " expected=" + reference + " actual=" + result.totalWeight());
            require(result.components() == 1, "Generated graph must be connected");
            require(result.edges().size() == n - 1, "MST must have n - 1 edges");
        }
        addRow(rows, "Kruskal", "undirected_" + density, n, m, kruskal, kruskal.result.totalWeight());
        addRow(rows, "Prim", "undirected_" + density, n, m, prim, prim.result.totalWeight());
        return kruskal.result.totalWeight();
    }

    private static long measureScc(int n, int m, String density, long reference, List<String> rows) {
        Graph graph = GraphGenerator.directed(n, m);
        var kosaraju = measure(metrics -> Kosaraju.run(graph, metrics));
        require(kosaraju.result.count() == reference, "SCC reference mismatch: " + n + "/" + m
            + " expected=" + reference + " actual=" + kosaraju.result.count());
        require(kosaraju.metrics.edgeScans == 2L * m, "Kosaraju must scan 2m edges");
        addRow(rows, "Kosaraju", "directed_" + density, n, m, kosaraju, kosaraju.result.count());
        return kosaraju.result.count();
    }

    private static long measureDag(int n, int m, String density, long reference, List<String> rows) {
        Graph graph = GraphGenerator.dag(n, m);
        Measurement<TopologicalResult> kahn = measure(metrics -> Kahn.run(graph, metrics));
        require(!kahn.result.hasCycle() && Kahn.isValid(graph, kahn.result.order()), "Invalid topological order");
        require(kahn.metrics.edgeScans == m, "Kahn must scan m edges");
        var shortest = measure(metrics -> DagShortestPath.run(graph, metrics));
        require(shortest.result.sumDistances() == reference, "DAG reference mismatch: " + n + "/" + m
            + " expected=" + reference + " actual=" + shortest.result.sumDistances());
        require(shortest.metrics.edgeScans == m, "DAG relaxation must scan m edges");
        addRow(rows, "Kahn", "dag_" + density, n, m, kahn, kahn.result.order().size());
        addRow(rows, "DagShortestPath", "dag_" + density, n, m, shortest, shortest.result.sumDistances());
        return shortest.result.sumDistances();
    }

    private static <T> Measurement<T> measure(Function<Metrics, T> algorithm) {
        long[] times = new long[RUNS];
        Metrics firstMetrics = null;
        T result = null;
        for (int run = 0; run < RUNS; run++) {
            Metrics metrics = new Metrics();
            long start = System.nanoTime();
            result = algorithm.apply(metrics);
            times[run] = System.nanoTime() - start;
            if (firstMetrics == null) firstMetrics = metrics;
            else require(sameCounters(firstMetrics, metrics), "Non-reproducible operation counters");
        }
        Arrays.sort(times);
        return new Measurement<>(result, firstMetrics, times[RUNS / 2] / 1_000_000.0);
    }

    private static boolean sameCounters(Metrics a, Metrics b) {
        return a.dsuFinds == b.dsuFinds && a.dsuUnions == b.dsuUnions
            && a.edgeScans == b.edgeScans && a.comparisons == b.comparisons && a.maxDepth == b.maxDepth;
    }

    private static void addRow(List<String> rows, String algorithm, String graph, int n, int m,
                               Measurement<?> measured, long result) {
        Metrics metrics = measured.metrics;
        rows.add(String.format(Locale.ROOT, "%s,%s,%d,%d,%.6f,%d,%d,%d,%d,%d",
            algorithm, graph, n, m, measured.medianMilliseconds, metrics.dsuOps(), metrics.edgeScans,
            metrics.comparisons, metrics.maxDepth, result));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}

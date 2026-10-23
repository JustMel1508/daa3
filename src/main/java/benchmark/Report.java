package benchmark;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Rebuilds the report from measured CSV data, without external libraries. */
public final class Report {
    private static final String HEADER =
            "algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result";
    private static final List<String> ALGORITHMS =
            List.of("Kruskal", "Prim", "Kosaraju", "Kahn", "DagShortestPath");
    private static final int[] SIZES = {1000, 3162, 10000, 31623, 100000};
    private static final String PAGE_BREAK = "\n<!-- PAGEBREAK -->\n\n";

    private Report() { }

    private record Row(String algorithm, String graph, int n, int m, double time,
                       long dsu, long scans, long comparisons, int depth, long result) {
        String density() { return graph.endsWith("_sparse") ? "sparse" : "dense"; }
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 1) throw new IllegalArgumentException("Usage: Report [results/results.csv]");
        Path input = Path.of(args.length == 0 ? "results/results.csv" : args[0]);
        List<Row> rows = read(input);
        StringBuilder out = new StringBuilder();
        pageOne(out);
        out.append(PAGE_BREAK);
        pageTwo(out, rows);
        out.append(PAGE_BREAK);
        pageThree(out, rows);
        out.append(PAGE_BREAK);
        pageFour(out);
        out.append(PAGE_BREAK);
        pageFive(out, rows);
        Files.writeString(Path.of("REPORT.md"), out.toString(), StandardCharsets.UTF_8);
        System.out.println("Saved REPORT.md: 50 CSV rows, measured ratios, fitted slopes, 5 logical pages.");
    }

    private static List<Row> read(Path input) throws IOException {
        List<String> lines = Files.readAllLines(input, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).equals(HEADER)) {
            throw new IOException("Unexpected CSV header in " + input);
        }
        List<Row> rows = new ArrayList<>();
        Set<String> identities = new HashSet<>();
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) continue;
            String[] fields = lines.get(i).split(",", -1);
            if (fields.length != 10) throw new IOException("Invalid CSV row " + (i + 1));
            try {
                Row row = new Row(fields[0], fields[1], Integer.parseInt(fields[2]),
                        Integer.parseInt(fields[3]), Double.parseDouble(fields[4]),
                        Long.parseLong(fields[5]), Long.parseLong(fields[6]),
                        Long.parseLong(fields[7]), Integer.parseInt(fields[8]), Long.parseLong(fields[9]));
                if (!ALGORITHMS.contains(row.algorithm())
                        || !(row.graph().endsWith("_sparse") || row.graph().endsWith("_dense"))
                        || row.m() != row.n() * (row.density().equals("sparse") ? 4 : 20)
                        || !Double.isFinite(row.time()) || row.time() < 0
                        || row.dsu() < 0 || row.scans() < 0 || row.comparisons() < 0 || row.depth() < 0) {
                    throw new IOException("Invalid measurement at CSV row " + (i + 1));
                }
                if (!identities.add(row.algorithm() + ":" + row.n() + ":" + row.density())) {
                    throw new IOException("Duplicate measurement at CSV row " + (i + 1));
                }
                rows.add(row);
            } catch (NumberFormatException exception) {
                throw new IOException("Invalid number at CSV row " + (i + 1), exception);
            }
        }
        if (rows.size() != 50) throw new IOException("The full benchmark must contain exactly 50 rows");
        for (int n : SIZES) {
            for (String density : List.of("sparse", "dense")) {
                for (String algorithm : ALGORITHMS) find(rows, algorithm, n, density);
                Row kruskal = find(rows, "Kruskal", n, density);
                Row prim = find(rows, "Prim", n, density);
                Row scc = find(rows, "Kosaraju", n, density);
                Row kahn = find(rows, "Kahn", n, density);
                Row dag = find(rows, "DagShortestPath", n, density);
                if (kruskal.result() != prim.result() || scc.scans() != 2L * scc.m()
                        || kahn.scans() != kahn.m() || dag.scans() != dag.m() || kahn.result() != n) {
                    throw new IOException("CSV invariants failed for n=" + n + ", " + density);
                }
            }
        }
        return rows;
    }

    private static void pageOne(StringBuilder out) {
        out.append("""
                # Assignment 3: Graphs, DSU, MST, SCC and Shortest Paths

                **Student:** Aisha Kazbekova | **Group:** SE-2518
                """);
        out.append("**Report generated:** ").append(LocalDate.now()).append("\n\n");
        out.append("""
                ## 1. Scope and reproducibility

                The city-road problem requires a cheapest spanning network; the scheduler requires
                dependency ordering, deadlock groups and minimum-cost routes. One adjacency-list
                `Graph` supports directed and undirected integer-weight graphs. DSU, Kruskal, lazy
                Prim, iterative Kosaraju, Kahn and DAG shortest paths are implemented directly.
                File input accepts `n m directed|undirected` followed by `u v w` edges. Disconnected
                MST inputs produce forests, and loops, parallel edges and isolated vertices are supported.

                The full experiment uses five vertex counts, two densities (`E=4V` and `E=20V`),
                and three graph kinds at every setting. Each generator starts with `new Random(42)`;
                the undirected generator first builds a shuffled random spanning tree. Benchmark
                assertions check both MST weights, the published result values, valid topological
                orders and exact traversal counters. Each recorded time is the median of five runs;
                counters must agree across those runs. The CSV contains 50 algorithm/graph rows.

                Reproduce with `mvn clean test`, then `mvn exec:java -Dexec.mainClass=benchmark.Benchmark`,
                `mvn exec:java -Dexec.mainClass=benchmark.Plotter`, and
                `mvn exec:java -Dexec.mainClass=benchmark.Report`. Run commands from the project root.

                ## 2. Complexity and counted work

                Here `V=n`, `E=m`; extra space excludes the original graph but includes algorithm
                data and returned structures. Logarithms use a minimum argument of two for empty cases.

                | Algorithm | Time | Extra space | Justification |
                |---|---|---|---|
                | DSU find / union | Amortized O(alpha(V)); makeSet O(V) | O(V) structure, O(1) per call | Union by size limits height; iterative path compression flattens paths. |
                | Kruskal | O(V + E log E + E alpha(V)) | O(V + E) | Copy and sort edges, then test endpoints using DSU. |
                | Lazy Prim | O(V + E log E) | O(V + E) | Candidate-edge heap can hold O(E) entries, including stale entries. |
                | Kosaraju | O(V + E) | O(V + E) | Two DFS passes and an explicitly stored transpose. |
                | Kahn | O(V + E) | O(V) | Build indegrees; enqueue each vertex once and process its edges. |
                | DAG shortest paths | O(V + E) | O(V) | Topological preparation followed by one relaxation pass. |

                For Prim, `log E` is stated explicitly because parallel edges are allowed. For
                simple graphs the usual `log V` formulation is equivalent up to a constant factor.

                Counters are incremented inside the implementations. `dsu_ops` counts find calls
                plus successful unions, not individual parent-pointer steps. `comparisons` counts
                sorting/heap weight comparisons, or candidate-distance comparisons for DAG paths.
                `edge_scans` counts Kruskal's examined candidates, Prim's adjacency entries, the two
                Kosaraju DFS passes, Kahn's removal loop, or DAG relaxations. Transpose/indegree
                construction and DAG's internal Kahn preparation are excluded from this scan counter
                but included in algorithm runtime and complexity. `max_depth` records the deepest
                explicit DFS stack; zero for algorithms without one.
                """);
    }

    private static void pageTwo(StringBuilder out, List<Row> rows) {
        out.append("""
                ## 3. Results and normalized counters

                These values are read from `results/results.csv`. The benchmark checks each against
                the course handout before saving the file, and checks Kruskal equals Prim.

                | V | E | MST weight | SCC count | Sum of DAG distances |
                |---:|---:|---:|---:|---:|
                """);
        for (int n : SIZES) {
            for (String density : List.of("sparse", "dense")) {
                Row k = find(rows, "Kruskal", n, density);
                Row s = find(rows, "Kosaraju", n, density);
                Row d = find(rows, "DagShortestPath", n, density);
                out.append(String.format(Locale.ROOT, "| %d | %d | %d | %d | %d |%n",
                        n, k.m(), k.result(), s.result(), d.result()));
            }
        }
        out.append("""

                Let `C` be comparisons, `D` DSU operations, and `S` edge scans; K and P denote
                Kruskal and Prim. Ratios below use actual measurements, rounded to four decimals.

                | V | Density | K: C/(E log2 E) | K: D/S | P: C/(E log2 E) | K: S/E |
                |---:|---|---:|---:|---:|---:|
                """);
        for (int n : SIZES) {
            for (String density : List.of("sparse", "dense")) {
                Row k = find(rows, "Kruskal", n, density);
                Row p = find(rows, "Prim", n, density);
                double formula = k.m() * log2(k.m());
                out.append(String.format(Locale.ROOT, "| %d | %s | %.4f | %.4f | %.4f | %.4f |%n",
                        n, density, k.comparisons() / formula,
                        (double) k.dsu() / k.scans(), p.comparisons() / formula,
                        (double) k.scans() / k.m()));
            }
        }
        out.append("""

                A roughly bounded comparison/formula ratio supports the expected sorting or heap
                growth; it need not be numerically identical at each size. The fixed weight range
                creates duplicate keys, and input order affects sorting and heap behavior.

                For all five sizes, Kosaraju has `S/(V+E)=2c/(1+c)`: **1.6000** at `c=4` and
                **1.9048** at `c=20`. Kahn and DAG relaxation have `S/(V+E)=c/(1+c)`:
                **0.8000** and **0.9524** respectively. Prim has exactly `S/E=2`. These constant
                ratios confirm linear scanning for each fixed density.

                Kruskal performs two find calls per examined edge and one successful union per
                selected tree edge, so `D/S=2+(V-1)/S` on these connected inputs. This ratio checks
                the implementation's call accounting; it cannot establish the inverse-Ackermann
                cost inside find. That amortized guarantee follows from the two DSU heuristics.
                Early stopping changes `S/E`, whereas sorting still considers the entire edge list.
                """);
    }

    private static void pageThree(StringBuilder out, List<Row> rows) {
        out.append("""
                ## 4. Correctness arguments and algorithm choice

                ### Cut property: Kruskal and Prim

                Take a cut that respects the edges already chosen, meaning no chosen edge crosses it.
                Let e be a minimum-weight edge crossing that cut, and consider an MST containing
                the chosen forest. If that MST already contains e, adding e is safe immediately.
                Otherwise, adding e creates a cycle whose path must contain another crossing edge f.
                Since e is lightest across the cut, replacing f with e cannot increase the tree's weight
                and preserves a spanning tree containing the previous choices. Kruskal applies this
                argument to distinct DSU components when it examines edges in weight order, while
                Prim applies it to the cut between visited and unvisited vertices. Repeating the same
                argument independently inside every connected component proves correctness for forests;
                ties and parallel edges can change the chosen tree but not the optimal total weight.

                ### Induction on a topological order

                Add an imaginary source with a zero-weight edge to every original indegree-zero vertex.
                Every vertex of a finite DAG is reachable from one of these original sources, because
                following predecessors cannot continue indefinitely without either stopping or forming a cycle.
                The base case is correct because every original source starts at distance zero.
                Assume all vertices preceding v in a topological order have their correct shortest distances.
                Every edge entering v comes from one of those earlier vertices, so its relaxation has already
                considered the best path through that predecessor. The minimum of those candidates is
                therefore the shortest distance to v, and no later vertex can supply another incoming edge.
                This proves one-pass relaxation, while `parent[]` records a predecessor realizing each
                improvement and reconstructs the selected path back to a source.

                Negative weights are safe here because a DAG contains no cycle, including a negative
                cycle, and topological dependencies remain valid. Negative edges are not universally
                forbidden in general graphs: Bellman-Ford supports them and detects reachable negative
                cycles. Dijkstra's usual greedy correctness argument requires nonnegative weights.

                | Situation | Choice | Reason |
                |---|---|---|
                | Sparse undirected graph, edge list | Kruskal | Sorting plus DSU is simple; no adjacency construction is required. |
                | Undirected adjacency list | Prim | Grow a component through available neighbors; restart for a forest. |
                | Dense simple graph | Matrix Prim, if available | O(V squared) avoids a large heap; this project implements the required heap version. |
                | Directed graph, cycles/deadlocks possible | Kosaraju | Groups mutually reachable jobs; a self-loop can also indicate a cyclic singleton. |
                | Need a dependency order or cycle detection | Kahn | Processes zero-indegree jobs; a short order signals a cycle. |
                | Weighted DAG, including negative edges | DAG shortest paths | One topological pass, O(V+E). |
                | General graph with negative edges | Bellman-Ford | Topological processing is unavailable; negative-cycle checks are necessary. |

                JUnit coverage includes a simple DSU reference, 100 random MST comparisons, brute-force
                SCC reachability, valid/cyclic orders, Bellman-Ford comparisons on negative-weight DAGs,
                and a 100,000-vertex path for stack safety. File fixtures also exercise disconnected and
                cyclic graphs. Graph type determines algorithm eligibility before density determines efficiency.

                ### Measured time

                """);
        out.append(String.format(Locale.ROOT,
                "Across the algorithms and densities, recorded medians at V=1,000 range from %.3f to %.3f ms; "
                + "at V=100,000 they range from %.3f to %.3f ms. ",
                minTime(rows, 1000), maxTime(rows, 1000), minTime(rows, 100000), maxTime(rows, 100000)));
        out.append("Report-generation environment: Java ")
                .append(System.getProperty("java.version")).append(", ")
                .append(System.getProperty("os.name")).append(" ")
                .append(System.getProperty("os.arch")).append(". ");
        out.append("The CSV is the measured-run record. JIT compilation, allocation, garbage collection, "
                + "hardware and background load affect these values, especially at the smallest sizes; "
                + "five-run medians reduce noise but do not make a universal speed ranking. "
                + "Operation counts, graph representation and asymptotic reasoning are the primary evidence.\n");
    }

    private static void pageFour(StringBuilder out) {
        out.append("""
                ## 5. Operation growth and scans per edge

                ![Operations versus vertex count, log-log axes](results/plots/operations_vs_n.png)

                **Figure 1.** Each series is one algorithm and density. Operations are the sum of
                `dsu_ops + edge_scans + comparisons`, an explicit instrumentation convention rather
                than a count of every CPU instruction. DSU calls and comparisons are different units,
                so this aggregate describes growth and must not be treated as a calibrated runtime model.
                Kosaraju and DAG shortest paths overlap at `2E` operations: Kosaraju scans twice,
                while DAG relaxation records one scan and one distance comparison per edge.

                ![Scans per edge versus vertex count](results/plots/scans_per_edge.png)

                **Figure 2.** Kosaraju and Prim stay at two scans per stored edge; Kahn and DAG
                relaxation stay at one. Their coincident lines are expected consequences of the
                counter definitions. Kruskal can stop once its DSU reaches one component, so its
                scan ratio need not be constant. Its initial sorting work is represented separately
                by comparisons; it does not disappear when the candidate scan ends early.
                """);
    }

    private static void pageFive(StringBuilder out, List<Row> rows) {
        out.append("""
                ## 6. Kruskal's examined share and fitted slopes

                ![Share of edges examined by Kruskal](results/plots/kruskal_edge_share.png)

                **Figure 3.** A smaller examined share means fewer DSU checks after sorting. It does
                not by itself prove that Kruskal is faster, because the full edge list was already
                sorted. Prim scans every adjacency entry and pays for heap operations; the decision
                therefore also depends on representation, density and queue/sorting comparison counts.

                Slopes below are ordinary least-squares fits of `log10(counter)` against `log10(V)`,
                using all five sizes independently for each density. A slope of one means that the
                measured counter grows linearly with V over this experiment.

                | Counter / algorithm | Sparse slope | Dense slope |
                |---|---:|---:|
                """);
        for (String algorithm : ALGORITHMS) {
            out.append(String.format(Locale.ROOT, "| edge_scans / %s | %.4f | %.4f |%n",
                    algorithm, slope(rows, algorithm, "sparse", false),
                    slope(rows, algorithm, "dense", false)));
        }
        out.append(String.format(Locale.ROOT, "| comparisons / Kruskal | %.4f | %.4f |%n",
                slope(rows, "Kruskal", "sparse", true), slope(rows, "Kruskal", "dense", true)));
        out.append("""

                The traversal scan slopes are 1.0000 because E is a fixed multiple of V. The handout
                also suggests that Kruskal's *edge-scan* slope should show its sorting logarithm;
                that mixes two different operations. Sorting compares weights, whereas Kruskal's
                subsequent candidate loop examines at most E edges, possibly fewer after early stopping.
                Its scan slope therefore reflects the examined share, not a direct measurement of
                `E log E`. The separate comparison slope is the appropriate empirical quantity to
                inspect for sorting work, without forcing a particular number.

                Even that fitted slope need not exactly equal a textbook prediction: `E log E` is a
                worst-case growth bound, only five sizes are sampled, and the generator's 1..1000
                weight range creates many ties. Java's sorting method and the generated ordering can
                affect the observed comparison growth. These limitations explain differences without
                changing, fabricating or deriving measured counters from complexity formulas.

                **Sources and traceability.** Requirements and reference outputs: the course handout
                `DAA_Assignment3_Graphs.pdf`, Assignment 3, sections 1-10. Implementations:
                `src/main/java/graph/`, `algorithms/`, `metrics/` and `benchmark/`. Measurements:
                `results/results.csv`. The three plots
                and every numeric report table are regenerated from that CSV. AI assistance was
                used for implementation, tests, explanations and documentation; see the README.
                """);
    }

    private static Row find(List<Row> rows, String algorithm, int n, String density) {
        return rows.stream().filter(row -> row.algorithm().equals(algorithm)
                && row.n() == n && row.density().equals(density)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing CSV row: "
                        + algorithm + "/" + n + "/" + density));
    }

    private static double minTime(List<Row> rows, int n) {
        return rows.stream().filter(row -> row.n() == n).mapToDouble(Row::time).min().orElseThrow();
    }

    private static double maxTime(List<Row> rows, int n) {
        return rows.stream().filter(row -> row.n() == n).mapToDouble(Row::time).max().orElseThrow();
    }

    private static double log2(double value) { return Math.log(value) / Math.log(2); }

    private static double slope(List<Row> rows, String algorithm, String density, boolean comparisons) {
        List<Row> selected = rows.stream().filter(row -> row.algorithm().equals(algorithm)
                && row.density().equals(density)).sorted(Comparator.comparingInt(Row::n)).toList();
        if (selected.size() != 5) throw new IllegalArgumentException("A slope requires all five sizes");
        double xMean = 0;
        double yMean = 0;
        for (Row row : selected) {
            long count = comparisons ? row.comparisons() : row.scans();
            if (count <= 0) throw new IllegalArgumentException("Cannot fit the logarithm of a nonpositive counter");
            xMean += Math.log10(row.n()) / selected.size();
            yMean += Math.log10(count) / selected.size();
        }
        double numerator = 0;
        double denominator = 0;
        for (Row row : selected) {
            double x = Math.log10(row.n()) - xMean;
            double y = Math.log10(comparisons ? row.comparisons() : row.scans()) - yMean;
            numerator += x * y;
            denominator += x * x;
        }
        return numerator / denominator;
    }
}

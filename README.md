# DAA Assignment 3 - Graphs

**Student:** Aisha Kazbekova  
**Group:** SE-2518  
**Course:** Design and Analysis of Algorithms  
**Release:** v1.0

## Requirements

- JDK 17 or newer.
- Maven 3.9+; IntelliJ IDEA's bundled Maven can also build this project.
- Internet access for Maven's first dependency download.
- About 2 GB of available Java heap for the largest benchmark. `.mvn/jvm.config` sets this for command-line Maven. For an IntelliJ Application run configuration, set VM options to `-Xmx2g`.

All graph algorithms are implemented in this repository. Java collections are used for storage and queues; no graph library is used. JUnit 5 is the only application test dependency. The PNG plotter uses Java's standard Java2D/ImageIO APIs, so Python is not required.

## Build and reproduce

Run all commands from the project root, where `pom.xml` is located:

```bash
mvn clean verify
mvn exec:java
```

The second command runs `benchmark.RunAll`: all 50 algorithm/graph measurements, three PNG figures, and a fresh `REPORT.md`. A failure of any published reference value, MST agreement, topological order, or exact traversal scan count stops the benchmark. The complete CSV is replaced only after all cases pass. The reference constants are assertions, never algorithm outputs.

The same complete workflow can be launched with one Maven command:

```bash
mvn clean verify exec:java
```

Alternatively, after packaging, run the application directly:

```bash
java -Xmx2g -Djava.awt.headless=true -jar target/daa-assignment3-1.0.jar
```

For a fast check of the two smallest size/density cases:

```bash
mvn exec:java -Dexec.args=--quick
```

Quick mode writes `results/quick.csv` (10 rows). It does not replace the required full `results/results.csv` (50 rows) or regenerate the full report. Rerun without `--quick` before submitting your own measurements.

To demonstrate file loading, disconnected forests, SCCs, cycles, negative weights and restored paths:

```bash
mvn exec:java -Dexec.mainClass=benchmark.Demo
```

Charts and the Markdown report can be recreated from an existing full CSV without rerunning the benchmark:

```bash
mvn exec:java -Dexec.mainClass=benchmark.Plotter
mvn exec:java -Dexec.mainClass=benchmark.Report
```

## IntelliJ IDEA

1. Choose **File > Open**, select this folder's `pom.xml`, and open it as a project.
2. Set **Project SDK** to JDK 17 or newer; let Maven finish importing JUnit and build plugins.
3. In the Maven tool window, run **Lifecycle > test** to execute the tests. Alternatively, right-click `src/test/java` and choose **Run 'All Tests'**.
4. Open `src/main/java/benchmark/RunAll.java`. Run its `main` method with the green triangle.
5. In **Run > Edit Configurations**, use the project root as the working directory and add `-Xmx2g` under VM options if needed.
6. Open `REPORT.md` with Markdown preview and inspect `results/plots/` after completion.

## Project layout

```text
src/main/java/graph       Shared adjacency-list graph and text reader
src/main/java/algorithms  DSU, Kruskal, Prim, Kosaraju, Kahn, DAG paths
src/main/java/metrics     Counters updated within algorithm operations
src/main/java/benchmark   Generator, self-checking benchmark, plots, report, demo
src/test/java/tests      JUnit 5 correctness and reference tests
data/                    Four small input graphs
results/results.csv      50 rows; median of five runs
results/plots/            Three generated PNG charts
REPORT.md                Report generated from the current full CSV
```

## Algorithms and important decisions

- A single `Graph` class supports directed and undirected weighted graphs. An undirected edge is stored once in the original edge list and twice in adjacency lists.
- DSU uses union by size and iterative full path compression. `find` calls and successful unions are counted separately; `dsuOps` is their sum.
- Kruskal sorts a copy of the original edges and stops when the connected graph's tree is complete. For disconnected inputs it examines all candidates and returns a forest.
- Prim uses a lazy priority queue of candidate edges. It starts at vertex 0 and restarts at each unvisited vertex, counting isolated vertices as components.
- Kosaraju uses explicit DFS frame stacks in both passes. It never uses recursive DFS, and its recorded stack depth is actual active stack size.
- Kahn reports a cycle when fewer than all vertices leave the queue. A separate validator checks vertex uniqueness and every edge's position in the order.
- DAG paths start every zero-indegree vertex at distance zero, as if a zero-cost super-source were present. Distances and MST sums use `long`; individual edge weights are `int`. Parents restore an actual path.

### Counter contract

`edge_scans` counts the main edge-processing phase, following the assignment's required exact counts. Kosaraju counts both DFS passes (`2m`), but not transpose construction. Kahn counts outgoing edges removed during queue processing (`m` on a DAG), but not indegree preparation. DAG shortest paths count relaxation edges (`m`), while topological preprocessing is timed but uses separate metrics. Prim examines two adjacency entries per undirected edge. Kruskal counts sorted candidates actually tested; sorting is recorded in `comparisons`.

`comparisons` counts each sort/heap weight comparison and each candidate-distance comparison during DAG relaxation. It does not include integer vertex-ID tie breakers. `max_depth` counts active frames in Kosaraju's own stack; unused depth counters are zero. The report distinguishes call counters from DSU's internal parent-pointer work.

### Test coverage

The suite has 20 JUnit tests across six classes. The tests include random DSU/reference comparisons; 100 connected MST comparisons; forests and unusual edges; SCC/BFS equivalence on small graphs; topological validation and cycle detection; 100 negative-weight DAG/Bellman-Ford comparisons; source initialization and restored paths; the supplied text files; exact reference results; and a directed path of 100,000 vertices.

## Reproducibility and limitations

Each graph generator begins with a new `Random(42)` and follows the handout's random-number call order. The ten size/density pairs each produce three graph kinds and five output rows. Timing is the median of five runs, including the measured algorithm's necessary preprocessing. Generation, validation, plotting, and explicit garbage collection between graph kinds are outside each timing interval. There is no separate warm-up phase; early timings may reflect JIT compilation. Compare counters and asymptotic reasoning, not one laptop's milliseconds.

The assignment's request for an `E log E` slope in Kruskal's **edge scans** is inconsistent with separately counted sorting **comparisons**. The report shows both actual scan slopes and comparison slopes and explains the difference. Optional bonus tasks (Tarjan and condensation/critical path) are not included.

## Git and submission

Use `main`, `feature/dsu`, `feature/mst`, `feature/scc`, `feature/paths`, and `feature/metrics`. Make commits after reviewing and checking the corresponding changes; keep their real timestamps. Merge working stages to `main`, then create the annotated release tag `v1.0`. Do not include `target/` or IDE-specific files in the submission. `START_HERE_RU.md` gives the exact staged commands.

Add your actual GitHub repository URL to `SUBMISSION.md` before the release commit. Upload the correctly named ZIP to Moodle and provide the repository link as required by the handout.

## AI-use acknowledgement

OpenAI ChatGPT assisted with implementation, automated tests, benchmark checks, chart/report generation, and explanations. This acknowledgement covers the supplied project rather than only proofreading. The student is responsible for reviewing the code, running it locally, understanding the algorithms, and explaining the work during the defense. The included CSV records an actual verification run; running `RunAll` replaces it with local measurements.

## Source

Course handout: Taubakabyl Nurlybek, *Design and Analysis of Algorithms, Assignment 3 - Graphs: DSU, MST, SCC and Shortest Paths*, deadline 23 October 2026. Required generator, reference values, rubric, and metric conventions follow that handout. The handout itself is not redistributed in this project.

GitHub repository: git remote -v

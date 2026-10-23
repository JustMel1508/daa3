package benchmark;

/** One entry point: reference-checked measurements, three charts, and a measured report. */
public final class RunAll {
    private RunAll() { }

    public static void main(String[] args) throws Exception {
        Benchmark.main(args);
        if (args.length == 1 && args[0].equals("--quick")) {
            System.out.println("Quick check finished. Run without arguments for all required deliverables.");
            return;
        }
        Plotter.main(new String[0]);
        Report.main(new String[0]);
        System.out.println("DONE: results/results.csv, results/plots/*.png and REPORT.md are ready.");
    }
}

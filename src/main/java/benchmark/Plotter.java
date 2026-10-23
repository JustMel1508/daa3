package benchmark;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;
import javax.imageio.ImageIO;

/** Portable PNG charts using Java's standard library; no Python or plotting dependency. */
public final class Plotter {
    private static final int WIDTH = 1440;
    private static final int HEIGHT = 960;
    private static final int LEFT = 140;
    private static final int RIGHT = 1375;
    private static final int TOP = 155;
    private static final int BOTTOM = 700;
    private static final int[] N_TICKS = {1000, 3162, 10000, 31623, 100000};
    private static final String[] ALGORITHMS = {"Kruskal", "Prim", "Kosaraju", "Kahn", "DagShortestPath"};
    private static final String[] LABELS = {"Kruskal", "Prim", "Kosaraju", "Kahn", "DAG shortest paths"};
    // Colors remain distinct with common color-vision deficiencies; markers distinguish overlap.
    private static final Color[] COLORS = {
        new Color(0x0072B2), new Color(0xD55E00), new Color(0x009E73),
        new Color(0xCC79A7), new Color(0x7B6100)
    };
    private static final Color INK = new Color(0x183247);
    private static final Color MUTED = new Color(0x536777);
    private static final Color GRID = new Color(0xDFE6EB);
    private static final BasicStroke SOLID = new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke DASHED = new BasicStroke(3.2f, BasicStroke.CAP_BUTT,
        BasicStroke.JOIN_ROUND, 10, new float[]{11, 8}, 0);
    private static final DecimalFormat NUMBER = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private Plotter() { }

    private record Row(String algorithm, String graph, int n, int m,
                       long dsuOps, long edgeScans, long comparisons) {
        double operations() { return (double) dsuOps + edgeScans + comparisons; }
        double scansPerEdge() { return edgeScans / (double) m; }
        boolean dense() { return graph.endsWith("_dense"); }
    }

    private record Series(int algorithm, boolean dense, List<Row> points) { }

    public static void main(String[] args) throws IOException {
        if (args.length > 1) throw new IllegalArgumentException("Usage: Plotter [path/to/results.csv]");
        Path csv = Path.of(args.length == 0 ? "results/results.csv" : args[0]);
        List<Row> rows = read(csv);
        Path destination = Path.of("results", "plots");
        Files.createDirectories(destination);
        draw(rows, Row::operations, true, false,
            "Counted work grows with graph size",
            "Operations = DSU calls + edge scans + weight comparisons; one curve per algorithm and density.",
            "Counted operations (log scale)",
            "Counters come from the algorithms. Coincident curves may overlap; timing is not used in this chart.",
            destination.resolve("operations_vs_n.png"));
        draw(rows, Row::scansPerEdge, false, false,
            "Edge scans per input edge",
            "A flat curve means a constant number of scans per edge as the input grows.",
            "Edge scans / m",
            "Shared levels: Prim and Kosaraju = 2; Kahn and DAG relaxation = 1. Preprocessing scans are excluded.",
            destination.resolve("scans_per_edge.png"));
        draw(rows, Row::scansPerEdge, false, true,
            "How many sorted edges Kruskal examines",
            "Kruskal stops after accepting n - 1 edges; all m edges are still sorted before scanning.",
            "Kruskal edge scans / m",
            "A lower scan share saves DSU work; it does not remove the sorting cost. Both densities use seed 42.",
            destination.resolve("kruskal_edge_share.png"));
        System.out.println("Saved three PNG charts to " + destination);
    }

    private static List<Row> read(Path csv) throws IOException {
        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        if (lines.size() < 2) throw new IllegalArgumentException("CSV has no data rows: " + csv);
        String expected = "algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result";
        if (!lines.get(0).equals(expected)) throw new IllegalArgumentException("Unexpected CSV header");
        List<Row> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) continue;
            String[] fields = lines.get(i).split(",", -1);
            if (fields.length != 10) throw new IllegalArgumentException("Bad CSV row " + (i + 1));
            Row row = new Row(fields[0], fields[1], Integer.parseInt(fields[2]), Integer.parseInt(fields[3]),
                Long.parseLong(fields[5]), Long.parseLong(fields[6]), Long.parseLong(fields[7]));
            if (row.n <= 0 || row.m <= 0) throw new IllegalArgumentException("Log charts require positive n and m");
            rows.add(row);
        }
        return rows;
    }

    private static List<Series> series(List<Row> rows, boolean onlyKruskal) {
        List<Series> result = new ArrayList<>();
        for (int algorithm = 0; algorithm < (onlyKruskal ? 1 : ALGORITHMS.length); algorithm++) {
            String name = ALGORITHMS[algorithm];
            // Dashed first, solid second. Markers and the note identify coincident curves.
            for (boolean dense : new boolean[]{true, false}) {
                List<Row> points = new ArrayList<>();
                for (Row row : rows) if (row.algorithm.equals(name) && row.dense() == dense) points.add(row);
                points.sort(Comparator.comparingInt(Row::n));
                if (!points.isEmpty()) result.add(new Series(algorithm, dense, points));
            }
        }
        return result;
    }

    private static void draw(List<Row> rows, ToDoubleFunction<Row> value, boolean logarithmicY,
                             boolean onlyKruskal, String title, String subtitle, String yLabel,
                             String note, Path output) throws IOException {
        List<Series> lines = series(rows, onlyKruskal);
        double maximum = 0;
        double minimum = Double.POSITIVE_INFINITY;
        for (Series line : lines) for (Row row : line.points) {
            double current = value.applyAsDouble(row);
            maximum = Math.max(maximum, current);
            if (current > 0) minimum = Math.min(minimum, current);
        }
        if (lines.isEmpty()) throw new IllegalArgumentException("No series to plot");
        double yMin = logarithmicY ? Math.floor(Math.log10(minimum)) : 0;
        double yMax = logarithmicY ? Math.ceil(Math.log10(maximum)) : (onlyKruskal ? 1.1 : 2.2);
        if (yMax <= yMin) yMax = yMin + 1;
        if (!logarithmicY) yMax = Math.max(yMax, Math.ceil(maximum * 1.1 * 10) / 10);

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        g.setColor(INK);
        g.drawString(title, 74, 65);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        g.setColor(MUTED);
        g.drawString(subtitle, 76, 101);

        axes(g, logarithmicY, yMin, yMax, yLabel, onlyKruskal);
        Shape originalClip = g.getClip();
        g.clip(new Rectangle2D.Double(LEFT - 9, TOP - 9, RIGHT - LEFT + 18, BOTTOM - TOP + 18));
        for (Series line : lines) {
            Color color = onlyKruskal && line.dense ? COLORS[1] : COLORS[line.algorithm];
            g.setColor(color);
            g.setStroke(line.dense ? DASHED : SOLID);
            Path2D path = new Path2D.Double();
            boolean first = true;
            for (Row row : line.points) {
                double x = x(row.n);
                double y = y(value.applyAsDouble(row), logarithmicY, yMin, yMax);
                if (first) path.moveTo(x, y); else path.lineTo(x, y);
                first = false;
            }
            g.draw(path);
            for (Row row : line.points) {
                marker(g, x(row.n), y(value.applyAsDouble(row), logarithmicY, yMin, yMax),
                    onlyKruskal ? (line.dense ? 1 : 0) : line.algorithm, line.dense, color);
            }
        }
        g.setClip(originalClip);
        legend(g, onlyKruskal);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        g.setColor(MUTED);
        centered(g, note, WIDTH / 2, 935);
        g.dispose();
        if (!ImageIO.write(image, "png", output.toFile())) throw new IOException("PNG writer unavailable");
    }

    private static void axes(Graphics2D g, boolean logY, double minY, double maxY,
                             String yLabel, boolean onlyKruskal) {
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        for (int tick : N_TICKS) {
            int x = (int) Math.round(x(tick));
            g.setColor(GRID);
            g.setStroke(new BasicStroke(1));
            g.drawLine(x, TOP, x, BOTTOM);
            g.setColor(MUTED);
            centered(g, NUMBER.format(tick), x, BOTTOM + 32);
        }
        if (logY) {
            for (int power = (int) minY; power <= (int) maxY; power++) {
                double position = BOTTOM - (power - minY) / (maxY - minY) * (BOTTOM - TOP);
                yTick(g, position, "10^" + power);
            }
        } else {
            double step = onlyKruskal ? 0.2 : 0.5;
            for (double tick = 0; tick <= maxY + 0.00001; tick += step) {
                double position = y(tick, false, minY, maxY);
                yTick(g, position, String.format(Locale.ROOT, "%.1f", tick));
            }
        }
        g.setColor(INK);
        g.setStroke(new BasicStroke(1.7f));
        g.drawLine(LEFT, TOP, LEFT, BOTTOM);
        g.drawLine(LEFT, BOTTOM, RIGHT, BOTTOM);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        centered(g, "Vertices n (log scale)", (LEFT + RIGHT) / 2, BOTTOM + 78);
        g.rotate(-Math.PI / 2);
        centered(g, yLabel, -(TOP + BOTTOM) / 2, 40);
        g.rotate(Math.PI / 2);
    }

    private static void yTick(Graphics2D g, double position, String label) {
        int y = (int) Math.round(position);
        g.setColor(GRID);
        g.setStroke(new BasicStroke(1));
        g.drawLine(LEFT, y, RIGHT, y);
        g.setColor(MUTED);
        int width = g.getFontMetrics().stringWidth(label);
        g.drawString(label, LEFT - 17 - width, y + 6);
    }

    private static void legend(Graphics2D g, boolean onlyKruskal) {
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        if (onlyKruskal) {
            legendEntry(g, 365, 841, COLORS[0], false, 0, "Sparse: m = 4n");
            legendEntry(g, 765, 841, COLORS[1], true, 1, "Dense: m = 20n");
            g.setColor(MUTED);
            centered(g, "Each marker is one measured input; the dashed line identifies the dense graphs.", WIDTH / 2, 887);
        } else {
            for (int i = 0; i < ALGORITHMS.length; i++) {
                legendEntry(g, 100 + i * 257, 834, COLORS[i], false, i, LABELS[i]);
            }
            legendEntry(g, 380, 881, INK, false, -1, "Sparse: m = 4n");
            legendEntry(g, 780, 881, INK, true, -1, "Dense: m = 20n");
        }
    }

    private static void legendEntry(Graphics2D g, int x, int y, Color color, boolean dense,
                                    int shape, String text) {
        g.setColor(color);
        g.setStroke(dense ? DASHED : SOLID);
        g.drawLine(x, y - 6, x + 43, y - 6);
        if (shape >= 0) marker(g, x + 21, y - 6, shape, dense, color);
        g.setColor(INK);
        g.drawString(text, x + 55, y);
    }

    private static void marker(Graphics2D g, double x, double y, int index, boolean hollow, Color color) {
        double radius = hollow ? 6.8 : 5.2;
        Shape shape;
        if (index == 0) shape = new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2);
        else if (index == 1) shape = new Rectangle2D.Double(x - radius, y - radius, radius * 2, radius * 2);
        else {
            Path2D polygon = new Path2D.Double();
            int sides = index == 2 ? 3 : index == 3 ? 4 : 5;
            for (int i = 0; i < sides; i++) {
                double angle = -Math.PI / 2 + i * 2 * Math.PI / sides;
                double px = x + Math.cos(angle) * radius * 1.2;
                double py = y + Math.sin(angle) * radius * 1.2;
                if (i == 0) polygon.moveTo(px, py); else polygon.lineTo(px, py);
            }
            polygon.closePath();
            shape = polygon;
        }
        g.setColor(hollow ? Color.WHITE : color);
        g.fill(shape);
        g.setColor(color);
        g.setStroke(new BasicStroke(2));
        g.draw(shape);
    }

    private static double x(int n) {
        return LEFT + (Math.log10(n) - 3) / 2 * (RIGHT - LEFT);
    }

    private static double y(double value, boolean log, double min, double max) {
        double transformed = log ? Math.log10(value) : value;
        return BOTTOM - (transformed - min) / (max - min) * (BOTTOM - TOP);
    }

    private static void centered(Graphics2D g, String text, int centerX, int baseline) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, baseline);
    }
}

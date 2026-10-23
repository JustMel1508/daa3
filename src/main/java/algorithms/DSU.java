package algorithms;

import java.util.Objects;
import metrics.Metrics;

/** Disjoint sets with union by size and iterative, full path compression. */
public final class DSU {
    private int[] parent;
    private int[] size;
    private int componentCount;
    private final Metrics metrics;

    public DSU(int n) { this(n, new Metrics()); }

    public DSU(int n, Metrics metrics) {
        this.metrics = Objects.requireNonNull(metrics);
        makeSet(n);
    }

    public void makeSet(int n) {
        if (n < 0) throw new IllegalArgumentException("Set count must be nonnegative");
        parent = new int[n];
        size = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            size[i] = 1;
        }
        componentCount = n;
    }

    public int find(int x) {
        checkElement(x);
        metrics.dsuFinds++;
        int root = x;
        while (parent[root] != root) root = parent[root];
        while (x != root) {
            int next = parent[x];
            parent[x] = root;
            x = next;
        }
        return root;
    }

    public boolean union(int x, int y) {
        int a = find(x);
        int b = find(y);
        if (a == b) return false;
        if (size[a] < size[b]) {
            int temporary = a;
            a = b;
            b = temporary;
        }
        parent[b] = a;
        size[a] += size[b];
        componentCount--;
        metrics.dsuUnions++;
        return true;
    }

    public int components() { return componentCount; }

    private void checkElement(int x) {
        if (x < 0 || x >= parent.length) throw new IllegalArgumentException("Invalid DSU element: " + x);
    }
}

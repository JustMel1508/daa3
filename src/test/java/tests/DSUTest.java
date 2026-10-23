package tests;

import algorithms.DSU;
import metrics.Metrics;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DSUTest {
    @Test
    void randomUnionsAgreeWithIndependentLabelReference() {
        int n = 100;
        int[] labels = new int[n];
        for (int i = 0; i < n; i++) labels[i] = i;
        int expectedComponents = n;
        long successfulUnions = 0;
        Metrics metrics = new Metrics();
        DSU dsu = new DSU(n, metrics);
        Random random = new Random(2718);

        for (int operation = 0; operation < 1500; operation++) {
            int a = random.nextInt(n);
            int b = random.nextInt(n);
            boolean expectedChange = labels[a] != labels[b];
            assertEquals(expectedChange, dsu.union(a, b));
            if (expectedChange) {
                int oldLabel = labels[b];
                int newLabel = labels[a];
                for (int i = 0; i < n; i++) {
                    if (labels[i] == oldLabel) labels[i] = newLabel;
                }
                expectedComponents--;
                successfulUnions++;
            }
            assertEquals(expectedComponents, dsu.components());
            for (int check = 0; check < 10; check++) {
                int x = random.nextInt(n);
                int y = random.nextInt(n);
                assertEquals(labels[x] == labels[y], dsu.find(x) == dsu.find(y));
            }
        }
        assertEquals(successfulUnions, metrics.dsuUnions);
        assertEquals(metrics.dsuFinds + metrics.dsuUnions, metrics.dsuOps());
        long before = metrics.dsuFinds;
        dsu.find(0);
        assertEquals(before + 1, metrics.dsuFinds);
    }

    @Test
    void makeSetReinitializesThePartitionAndRepeatedUnionDoesNotMerge() {
        DSU dsu = new DSU(4);
        assertTrue(dsu.union(0, 1));
        assertFalse(dsu.union(1, 0));
        assertFalse(dsu.union(2, 2));
        assertEquals(3, dsu.components());
        dsu.makeSet(6);
        assertEquals(6, dsu.components());
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                assertEquals(i == j, dsu.find(i) == dsu.find(j));
            }
        }
    }
}

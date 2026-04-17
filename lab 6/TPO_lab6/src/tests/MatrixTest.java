import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;

public class MatrixTest {

    static final double EPS = 1e-9;

    // множення: subA (rows×n) × matB (n×n) = subC (rows×n)
    static double[] multiply(double[] subA, double[] matB, int rows, int n) {
        double[] subC = new double[rows * n];
        for (int i = 0; i < rows; i++) {
            for (int k = 0; k < n; k++) {
                double sum = 0.0;
                for (int j = 0; j < n; j++) {
                    sum += subA[i * n + j] * matB[j * n + k];
                }
                subC[i * n + k] = sum;
            }
        }
        return subC;
    }

    // генерація матриці (детермінована, з фіксованим seed)
    static double[] seededMatrix(int size, long seed) {
        java.util.Random rng = new java.util.Random(seed);
        double[] data = new double[size];
        for (int i = 0; i < size; i++) {
            data[i] = -100 + 200 * rng.nextDouble();
        }
        return data;
    }

    // розподіл рядків між workers
    static int[] computeRowCounts(int n, int numWorkers) {
        int base  = n / numWorkers;
        int extra = n % numWorkers;
        int[] counts = new int[numWorkers];
        for (int i = 0; i < numWorkers; i++) {
            counts[i] = (i < extra) ? base + 1 : base;
        }
        return counts;
    }

    static int[] computeOffsets(int[] rowCounts) {
        int[] offsets = new int[rowCounts.length];
        for (int i = 1; i < rowCounts.length; i++) {
            offsets[i] = offsets[i - 1] + rowCounts[i - 1];
        }
        return offsets;
    }

    // повне послідовне множення
    static double[] fullMultiply(double[] A, double[] B, int n) {
        double[] C = new double[n * n];
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < n; k++) {
                double sum = 0.0;
                for (int j = 0; j < n; j++) {
                    sum += A[i * n + j] * B[j * n + k];
                }
                C[i * n + k] = sum;
            }
        }
        return C;
    }

    @Test
    // множення одиничної матриці 3×3 дає ту саму матрицю
    void testMultiplyIdentity() {
        int n = 3;
        double[] identity = {
                1, 0, 0,
                0, 1, 0,
                0, 0, 1
        };
        double[] matB = {
                2, 3, 4,
                5, 6, 7,
                8, 9, 1
        };
        double[] result = multiply(matB, identity, n, n);
        assertArrayEquals(matB, result, EPS);
    }

    @Test
    // паралельне множення збігається з послідовним
    void testParallelMatchesSequentialLarge() {
        int n = 50;
        int numWorkers = 5;

        double[] matA = seededMatrix(n * n, 10L);
        double[] matB = seededMatrix(n * n, 20L);

        double[] expected = fullMultiply(matA, matB, n);

        int[] rowCounts = computeRowCounts(n, numWorkers);
        int[] offsets = computeOffsets(rowCounts);
        double[] result = new double[n * n];

        for (int w = 0; w < numWorkers; w++) {
            int rows = rowCounts[w];
            int off = offsets[w];
            double[] subA = Arrays.copyOfRange(matA, off * n, (off + rows) * n);
            double[] subC = multiply(subA, matB, rows, n);
            System.arraycopy(subC, 0, result, off * n, rows * n);
        }

        assertArrayEquals(expected, result, EPS);
    }

    @Test
    // сума rowCounts завжди дорівнює n
    void testRowCountsSumEqualsN() {
        int[][] cases = {{10, 3}, {15, 4}, {100, 7}, {7, 7}, {6, 4}};
        for (int[] c : cases) {
            int n = c[0], workers = c[1];
            int[] counts = computeRowCounts(n, workers);
            int sum = Arrays.stream(counts).sum();
            assertEquals(n, sum,
                    String.format("n=%d workers=%d: sum=%d", n, workers, sum));
        }
    }

    @Test
    // рівний поділ без залишку
    void testRowCountsEvenSplit() {
        int[] counts = computeRowCounts(12, 4);
        assertArrayEquals(new int[]{3, 3, 3, 3}, counts);
    }

    @Test
    // нерівний поділ з залишком
    void testRowCountsUnevenSplit() {
        // 10 / 3 = 3 remainder 1 → [4, 3, 3]
        int[] counts = computeRowCounts(10, 3);
        assertArrayEquals(new int[]{4, 3, 3}, counts);
    }

    @Test
    // offsets починаються з 0 і монотонно зростають
    void testOffsetsMonotone() {
        int[] counts  = computeRowCounts(15, 4);
        int[] offsets = computeOffsets(counts);

        assertEquals(0, offsets[0]);
        for (int i = 1; i < offsets.length; i++) {
            assertTrue(offsets[i] > offsets[i - 1],
                    "offsets[" + i + "] має бути більше offsets[" + (i-1) + "]");
        }
    }

    @Test
    // offsets коректні для нерівного поділу
    void testOffsetsCorrect() {
        // counts = [4, 3, 3] → offsets = [0, 4, 7]
        int[] counts  = computeRowCounts(10, 3);
        int[] offsets = computeOffsets(counts);
        assertArrayEquals(new int[]{0, 4, 7}, offsets);
    }

    @Test
    // розмір згенерованої матриці правильний
    void testMatrixSize() {
        int size = 25;
        double[] m = seededMatrix(size, 0L);
        assertEquals(size, m.length);
    }

    @Test
    // матриці з різним seed відрізняються
    void testDifferentSeeds() {
        double[] m1 = seededMatrix(100, 1L);
        double[] m2 = seededMatrix(100, 2L);
        assertFalse(Arrays.equals(m1, m2));
    }

    @Test
    // матриці з однаковим seed збігаються
    void testSameSeedReproducible() {
        double[] m1 = seededMatrix(100, 42L);
        double[] m2 = seededMatrix(100, 42L);
        assertArrayEquals(m1, m2, EPS);
    }
}
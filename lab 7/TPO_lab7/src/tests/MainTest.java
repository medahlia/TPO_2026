import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MainTest {

    @Test
    void testMultiplySequential2x2() {
        double[] A = {
                1, 2,
                3, 4
        };

        double[] B = {
                5, 6,
                7, 8
        };

        double[] result = Main.multiplySequential(A, B, 2);

        double[] expected = {
                19, 22,
                43, 50
        };

        assertArrayEquals(expected, result, 0.0001);
    }

    @Test
    void testMultiplyBlock() {
        double[] A = {
                1, 2,
                3, 4
        };

        double[] B = {
                5, 6,
                7, 8
        };

        double[] result = Main.multiplyBlock(A, B, 2, 2);

        double[] expected = {
                19, 22,
                43, 50
        };

        assertArrayEquals(expected, result, 0.0001);
    }

    @Test
    void testRowCounts() {
        int[] result = Main.rowCounts(10, 3);

        int sum = 0;
        for (int r : result) {
            sum += r;
        }

        assertEquals(10, sum);
        assertEquals(3, result.length);
    }

    @Test
    void testOffsets() {
        int[] counts = {3, 3, 4};

        int[] offsets = Main.offsets(counts);

        int[] expected = {0, 3, 6};

        assertArrayEquals(expected, offsets);
    }

    @Test
    void testRandomMatrixSize() {
        double[] matrix = Main.randomMatrix(100, 42);

        assertEquals(100, matrix.length);
    }

    @Test
    void testRandomMatrixDeterministic() {
        double[] A = Main.randomMatrix(10, 42);
        double[] B = Main.randomMatrix(10, 42);

        assertArrayEquals(A, B, 0.000001);
    }
}
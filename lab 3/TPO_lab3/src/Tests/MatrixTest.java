import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


class MatrixTest {
    int size = 100;

    @Test
    // матриця створюється з правильними рядками та стовпцями
    void generateRandom_shouldCreateMatrixWithCorrectSize() {
        int rows = size;
        int cols = size;

        Matrix matrix = Matrix.generateRandom(rows, cols);

        assertEquals(rows, matrix.getRows(), "Кількість рядків повинна відповідати");
        assertEquals(cols, matrix.getCols(), "Кількість стовпців повинна відповідати");

        for (int i = 0; i < matrix.getRows(); i++) {
            for (int j = 0; j < matrix.getCols(); j++) {
                System.out.print(matrix.getValue(i, j) + " ");
            }
            System.out.println();
        }
    }

    @Test
    // матриця не пуста, тобто заповнена випадковими числами
    void generateRandom_shouldHaveNonZeroValues() {
        Matrix matrix = Matrix.generateRandom(size, size);

        boolean hasNonZero = false;

        for (int i = 0; i < matrix.getRows(); i++) {
            for (int j = 0; j < matrix.getCols(); j++) {
                if (matrix.getValue(i, j) != 0) {
                    hasNonZero = true;
                    break;
                }
            }
            if (hasNonZero) break;
        }

        assertTrue(hasNonZero, "Матриця повинна містити числа від 0 до 9");
    }

    @Test
    // кожен виклик створює новий об’єкт, а не повертає той самий
    void generateRandom_shouldAllowMultipleCalls() {
        Matrix m1 = Matrix.generateRandom(size, size);
        Matrix m2 = Matrix.generateRandom(size, size);

        // Можемо перевірити, що обʼєкти різні
        assertNotSame(m1, m2, "Кожен виклик має створювати новий обʼєкт матриці");
    }
}
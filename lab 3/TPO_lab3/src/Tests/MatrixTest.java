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

    // перевірка на коректність обчислення послідовного методу
    @Test
    void multiply_shouldReturnCorrectResult() {

        Matrix m1 = new Matrix(2, 2);
        m1.setValue(0, 0, 1);
        m1.setValue(0, 1, 2);
        m1.setValue(1, 0, 3);
        m1.setValue(1, 1, 4);

        Matrix m2 = new Matrix(2, 2);
        m2.setValue(0, 0, 5);
        m2.setValue(0, 1, 6);
        m2.setValue(1, 0, 7);
        m2.setValue(1, 1, 8);

        Matrix result = SequentialMethod.multiply(m1, m2);

        assertEquals(19, result.getValue(0,0));
        assertEquals(22, result.getValue(0,1));
        assertEquals(43, result.getValue(1,0));
        assertEquals(50, result.getValue(1,1));
    }

    // перевірка на коректність обчислення послідовного методу
    @Test
    void multiply_differentSizeMatrices_shouldReturnCorrectResult() {

        Matrix m1 = new Matrix(2, 3);
        m1.setValue(0, 0, 1);
        m1.setValue(0, 1, 2);
        m1.setValue(0, 2, 3);
        m1.setValue(1, 0, 4);
        m1.setValue(1, 1, 5);
        m1.setValue(1, 2, 6);

        Matrix m2 = new Matrix(3, 2);
        m2.setValue(0, 0, 7);
        m2.setValue(0, 1, 8);
        m2.setValue(1, 0, 9);
        m2.setValue(1, 1, 10);
        m2.setValue(2, 0, 11);
        m2.setValue(2, 1, 12);

        Matrix result = SequentialMethod.multiply(m1, m2);

        assertEquals(58, result.getValue(0,0));
        assertEquals(64, result.getValue(0,1));
        assertEquals(139, result.getValue(1,0));
        assertEquals(154, result.getValue(1,1));
    }

    @Test
    void testStripedMultiplicationMatchesSequential() {
        // Генеруємо випадкові матриці
        Matrix a = Matrix.generateRandom(size, size);
        Matrix b = Matrix.generateRandom(size, size);

        // Послідовне множення
        SequentialMethod seqMethod = new SequentialMethod();
        Matrix expected = seqMethod.multiply(a, b);

        // Паралельне множення (StripedMethod)
        StripedMethod stripedMethod = new StripedMethod();
        Matrix actual = stripedMethod.multiplyMatrix(a, b, 3); // 3 потоки

        // Перевіряємо, що всі елементи збігаються
        for (int i = 0; i < expected.getRows(); i++) {
            for (int j = 0; j < expected.getCols(); j++) {
                assertEquals(expected.getValue(i, j), actual.getValue(i, j),
                        "Елемент [" + i + "][" + j + "] не збігається");
            }
        }
    }

    @Test
    void testFoxMultiplicationCorrectness() {
        // Генеруємо дві сумісні матриці
        Matrix a = Matrix.generateRandom(size, size);
        Matrix b = Matrix.generateRandom(size, size);

        // Послідовне множення
        SequentialMethod seqMethod = new SequentialMethod();
        Matrix expected = seqMethod.multiply(a, b);

        // Множення через FoxMethod з 4 потоками
        FoxMethod foxMethod = new FoxMethod(a, b, 4);
        Matrix result = foxMethod.multiplyMatrix();

        // Перевіряємо, що кожен елемент збігається
        for (int i = 0; i < expected.getRows(); i++) {
            for (int j = 0; j < expected.getCols(); j++) {
                assertEquals(expected.getValue(i, j), result.getValue(i, j),
                        "Елемент [" + i + "," + j + "] не збігається");
            }
        }
    }

    @Test
    void testAllMethodsProduceSameResult() {
        // Створимо невеликі матриці для тесту
        Matrix a = Matrix.generateRandom(5, 4);
        Matrix b = Matrix.generateRandom(4, 6);

        // --- Sequential ---
        SequentialMethod seqMethod = new SequentialMethod();
        Matrix resSeq = seqMethod.multiply(a, b);

        // --- Striped ---
        StripedMethod stripedMethod = new StripedMethod();
        Matrix resStriped = stripedMethod.multiplyMatrix(a, b, 3);

        // --- Fox ---
        FoxMethod foxMethod = new FoxMethod(a, b, 3);
        Matrix resFox = foxMethod.multiplyMatrix();

        // Перевірка, що всі значення співпадають
        int rows = resSeq.getRows();
        int cols = resSeq.getCols();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                assertEquals(resSeq.getValue(i, j), resStriped.getValue(i, j),
                        "Mismatch at [" + i + "," + j + "] between Sequential and Striped");
                assertEquals(resSeq.getValue(i, j), resFox.getValue(i, j),
                        "Mismatch at [" + i + "," + j + "] between Sequential and Fox");
            }
        }
    }
}
public class SpeedUpTest {

    public static void main(String[] args) {
        int[] matrixSizes = {500, 1000, 1500, 2000, 2500, 3000};
        int[] threadsNum  = {2, 4, 8, 16, 32};

        // [розмір матриці][кількість потоків][метод: 0=seq, 1=striped, 2=fox]
        long[][][] testResults = new long[matrixSizes.length][threadsNum.length][3];

        for (int i = 0; i < matrixSizes.length; i++) {
            Matrix m1 = Matrix.generateRandom(matrixSizes[i], matrixSizes[i]);
            Matrix m2 = Matrix.generateRandom(matrixSizes[i], matrixSizes[i]);

            for (int j = 0; j < threadsNum.length; j++) {

                // Sequential
                long startTime = System.currentTimeMillis();
                Matrix res1 = SequentialMethod.multiply(m1, m2);
                testResults[i][j][0] = System.currentTimeMillis() - startTime;

                // Striped
                StripedMethod stripedMethod = new StripedMethod();
                startTime = System.currentTimeMillis();
                Matrix res2 = stripedMethod.multiplyMatrix(m1, m2, threadsNum[j]);
                testResults[i][j][1] = System.currentTimeMillis() - startTime;

                // Fox
                FoxMethod foxMethod = new FoxMethod(m1, m2, threadsNum[j]);
                startTime = System.currentTimeMillis();
                Matrix res3 = foxMethod.multiplyMatrix();
                testResults[i][j][2] = System.currentTimeMillis() - startTime;

                if (checkResults(res1, res2, res3)) {
                    System.out.println("Matrix size: " + matrixSizes[i] + "x" + matrixSizes[i]
                            + ", threads: " + threadsNum[j]);
                    System.out.println("  Sequential : " + testResults[i][j][0] + " ms");
                    System.out.println("  Striped    : " + testResults[i][j][1] + " ms"
                            + "  (speedup: " + String.format("%.2f", (double) testResults[i][j][0] / testResults[i][j][1]) + "x)");
                    System.out.println("  Fox        : " + testResults[i][j][2] + " ms"
                            + "  (speedup: " + String.format("%.2f", (double) testResults[i][j][0] / testResults[i][j][2]) + "x)");
                    System.out.println("-------------------");
                } else {
                    throw new IllegalArgumentException(
                            "Incorrect results for size " + matrixSizes[i] + ", threads " + threadsNum[j]);
                }
            }
        }
    }

    public static boolean checkResults(Matrix r1, Matrix r2, Matrix r3) {
        for (int i = 0; i < r1.getRows(); i++)
            for (int j = 0; j < r1.getCols(); j++)
                if (r1.getValue(i, j) != r2.getValue(i, j) || r1.getValue(i, j) != r3.getValue(i, j))
                    return false;
        return true;
    }
}
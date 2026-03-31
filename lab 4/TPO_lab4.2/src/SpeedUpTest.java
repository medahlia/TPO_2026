public class SpeedUpTest {

    public static void main(String[] args) {
        int[] matrixSizes = {512, 992, 1504, 2016, 2496, 3008};
        int[] threadsNum  = {2, 4, 8, 16, 32};

        long[][][] testResults = new long[matrixSizes.length][threadsNum.length][3];

        for (int i = 0; i < matrixSizes.length; i++) {
            Matrix m1 = Matrix.generateRandom(matrixSizes[i], matrixSizes[i]);
            Matrix m2 = Matrix.generateRandom(matrixSizes[i], matrixSizes[i]);

            warmUp(m1, m2);

            System.out.println("\nMatrix size: " + matrixSizes[i] + "x" + matrixSizes[i]);
            System.out.println("--------------------------------------------------------------------------------");
            System.out.printf("%-10s %-12s %-18s %-18s%n",
                    "Threads", "Sequential", "Striped (speedup)", "Fox (speedup)");
            System.out.println("--------------------------------------------------------------------------------");

            for (int j = 0; j < threadsNum.length; j++) {
                long startTime = System.currentTimeMillis();

                // Striped
                StripedMethod stripedMethod = new StripedMethod();
                startTime = System.currentTimeMillis();
                Matrix res2 = stripedMethod.multiplyMatrix(m1, m2, threadsNum[j]);
                testResults[i][j][1] = System.currentTimeMillis() - startTime;


                // Sequential

                Matrix res1 = SequentialMethod.multiply(m1, m2);
                testResults[i][j][0] = System.currentTimeMillis() - startTime;

                if (!checkResults(res1, res2)) {
                    throw new IllegalArgumentException(
                            "Incorrect results for size " + matrixSizes[i] + ", threads " + threadsNum[j]);
                }

                double stripedSpeedup =
                        (double) testResults[i][j][0] / testResults[i][j][1];

                double foxSpeedup =
                        (double) testResults[i][j][0] / testResults[i][j][2];

                System.out.printf("%-10d %-12s %-20s %-20s%n",
                        threadsNum[j],
                        testResults[i][j][0] + " ms",
                        testResults[i][j][1] + " ms (" + String.format("%.2f", stripedSpeedup) + "x)",
                        testResults[i][j][2] + " ms (" + String.format("%.2f", foxSpeedup) + "x)"
                );
            }

            System.out.println("--------------------------------------------------------------------------------");
        }
    }

    public static boolean checkResults(Matrix r1, Matrix r2) {
        for (int i = 0; i < r1.getRows(); i++)
            for (int j = 0; j < r1.getCols(); j++)
                if (r1.getValue(i, j) != r2.getValue(i, j))
                    return false;
        return true;
    }

    private static void warmUp(Matrix m1, Matrix m2) {
        SequentialMethod.multiply(m1, m2);

        StripedMethod striped = new StripedMethod();
        striped.multiplyMatrix(m1, m2, 4);

    }
}
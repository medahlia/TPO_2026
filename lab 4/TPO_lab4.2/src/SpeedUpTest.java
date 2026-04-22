public class SpeedUpTest {

    public static void main(String[] args) {
        int[] matrixSizes = {512, 992, 1504, 2016}; //{2496, 3008}; //{512, 992, 1504, 2016};
        int numProcessors = Runtime.getRuntime().availableProcessors();
        int iterations = 8;

        System.out.println("Available processors: " + numProcessors);

        for (int size : matrixSizes) {
            Matrix m1 = Matrix.generateRandom(size, size);
            Matrix m2 = Matrix.generateRandom(size, size);

            warmUp(m1, m2, numProcessors);

            long totalStripedTime   = 0;
            long totalStripedFJTime = 0;
            long totalFoxTime       = 0;
            long totalFoxFJTime     = 0;

            for (int i = 0; i < iterations; i++) {

                StripedMethod striped = new StripedMethod();

                // Striped (Thread)
                long start = System.nanoTime();
                Matrix resStriped = striped.multiplyMatrix(m1, m2, numProcessors);
                totalStripedTime += System.nanoTime() - start;

                // Striped (ForkJoin)
                start = System.nanoTime();
                Matrix resStripedFJ = striped.multiplyMatrixForkJoin(m1, m2, numProcessors);
                totalStripedFJTime += System.nanoTime() - start;

                // Fox (Thread)
                FoxMethod fox = new FoxMethod(m1, m2, numProcessors);
                start = System.nanoTime();
                Matrix resFox = fox.multiplyMatrix();
                totalFoxTime += System.nanoTime() - start;

                // Fox (ForkJoin)
                FoxMethod foxFJ = new FoxMethod(m1, m2, numProcessors);
                start = System.nanoTime();
                Matrix resFoxFJ = foxFJ.multiplyMatrixForkJoin();
                totalFoxFJTime += System.nanoTime() - start;

                if (!checkResults(resStriped, resStripedFJ) || !checkResults(resFox, resFoxFJ)) {
                    throw new IllegalArgumentException("Incorrect results for size " + size);
                }
            }

            double avgStriped   = (double) totalStripedTime   / iterations / 1e9;
            double avgStripedFJ = (double) totalStripedFJTime / iterations / 1e9;
            double avgFox       = (double) totalFoxTime       / iterations / 1e9;
            double avgFoxFJ     = (double) totalFoxFJTime     / iterations / 1e9;

            double stripedSpeedup = avgStriped / avgStripedFJ;
            double foxSpeedup     = avgFox     / avgFoxFJ;

            System.out.printf("%nMatrix size: %d x %d%n", size, size);
            System.out.println("--------------------------------------------------------------------------------");
            System.out.printf("%-30s %-15s %-15s %-10s%n", "Method", "Thread (avg)", "ForkJoin (avg)", "Speedup");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.printf("%-30s %-15s %-15s %-10s%n",
                    "Striped",
                    String.format("%.3f s", avgStriped),
                    String.format("%.3f s", avgStripedFJ),
                    String.format("%.3fx", stripedSpeedup));
            System.out.printf("%-30s %-15s %-15s %-10s%n",
                    "Fox",
                    String.format("%.3f s", avgFox),
                    String.format("%.3f s", avgFoxFJ),
                    String.format("%.3fx", foxSpeedup));
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

    private static void warmUp(Matrix m1, Matrix m2, int numProcessors) {
        StripedMethod striped = new StripedMethod();
        striped.multiplyMatrix(m1, m2, numProcessors);
        striped.multiplyMatrixForkJoin(m1, m2, numProcessors);

        FoxMethod fox = new FoxMethod(m1, m2, numProcessors);
        fox.multiplyMatrix();

        FoxMethod foxFJ = new FoxMethod(m1, m2, numProcessors);
        foxFJ.multiplyMatrixForkJoin();
    }
}
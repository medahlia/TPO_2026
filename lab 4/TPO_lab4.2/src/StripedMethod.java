import java.util.ArrayList;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ForkJoinPool;

public class StripedMethod {

    // звичайна реалізація
    public Matrix multiplyMatrix(Matrix a, Matrix b, int threadsCount) {

        if (a.getCols() != b.getRows())
            throw new IllegalArgumentException("It is impossible to multiply matrices due to their size");

        int aRows = a.getRows();
        int bCols = b.getCols();
        int commonDim = a.getCols();

        Matrix result = new Matrix(aRows, bCols);

        int rowsPerThread = aRows / threadsCount;
        int remainder = aRows % threadsCount;
        ArrayList<Thread> threads = new ArrayList<>();

        int startRow = 0;
        for (int i = 0; i < threadsCount; i++) {
            int rows = rowsPerThread + (i < remainder ? 1 : 0);
            int endRow = startRow + rows;
            final int sRow = startRow;
            final int eRow = endRow;

            threads.add(new Thread(() -> {
                for (int row = sRow; row < eRow; row++) {
                    for (int col = 0; col < bCols; col++) {
                        int sum = 0;
                        for (int k = 0; k < commonDim; k++) {
                            sum += a.getValue(row, k) * b.getValue(k, col);
                        }
                        result.setValue(row, col, sum);
                    }
                }
            }));

            startRow = endRow;
        }

        for (Thread thread : threads) thread.start();
        try {
            for (Thread thread : threads) thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return result;
    }

    // ForkJoin реалізація
    private static class StripedTask extends RecursiveAction {
        private static final int THRESHOLD = 32;

        private final Matrix a, b, result;
        private final int startRow, endRow;

        StripedTask(Matrix a, Matrix b, Matrix result, int startRow, int endRow) {
            this.a = a;
            this.b = b;
            this.result = result;
            this.startRow = startRow;
            this.endRow = endRow;
        }

        @Override
        protected void compute() {
            if (endRow - startRow <= THRESHOLD) {
                int bCols = b.getCols();
                int commonDim = a.getCols();
                for (int row = startRow; row < endRow; row++) {
                    for (int col = 0; col < bCols; col++) {
                        int sum = 0;
                        for (int k = 0; k < commonDim; k++) {
                            sum += a.getValue(row, k) * b.getValue(k, col);
                        }
                        result.setValue(row, col, sum);
                    }
                }
            } else {
                // рекурсивне розбиття
                int mid = (startRow + endRow) / 2;
                StripedTask left = new StripedTask(a, b, result, startRow, mid);
                StripedTask right = new StripedTask(a, b, result, mid, endRow);
                left.fork();
                right.compute();
                left.join();
            }
        }
    }

    public Matrix multiplyMatrixForkJoin(Matrix a, Matrix b, int threadsCount) {

        if (a.getCols() != b.getRows())
            throw new IllegalArgumentException("It is impossible to multiply matrices due to their size");

        Matrix result = new Matrix(a.getRows(), b.getCols());

        ForkJoinPool pool = new ForkJoinPool(threadsCount);
        pool.invoke(new StripedTask(a, b, result, 0, a.getRows()));
        pool.shutdown();

        return result;
    }
}
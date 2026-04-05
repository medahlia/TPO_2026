import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ForkJoinPool;
import java.util.ArrayList;

public class FoxMethod {

    private final Matrix a;
    private final Matrix b;
    private final int threadsNum;
    private final Matrix result;

    public FoxMethod(Matrix a, Matrix b, int threadsNum) {
        this.a = a;
        this.b = b;
        this.result = new Matrix(a.getRows(), b.getCols());

        if (threadsNum > a.getRows() * b.getCols() / 4) {
            this.threadsNum = Math.max(a.getRows() * b.getCols() / 4, 1);
        } else {
            this.threadsNum = Math.max(threadsNum, 1);
        }
    }

    // звичайна реалізація
    public Matrix multiplyMatrix() {
        int blockSize = (int) Math.ceil(1.0 * a.getRows() / (int) Math.sqrt(threadsNum));

        FoxProcessThread[] threads = new FoxProcessThread[threadsNum];
        int threadCounter = 0;

        for (int i = 0; i < a.getRows(); i += blockSize)
            for (int j = 0; j < b.getCols(); j += blockSize)
                threads[threadCounter++] = new FoxProcessThread(a, b, i, j, blockSize, result);

        for (int i = 0; i < threadCounter; i++) threads[i].start();
        for (int i = 0; i < threadCounter; i++) {
            try { threads[i].join(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        return result;
    }

    // ForkJoin реалізація
    private static class FoxTask extends RecursiveAction {
        private static final int BLOCK_THRESHOLD = 64;

        private final Matrix a, b, result;
        private final int rowStart, rowEnd, colStart, colEnd;

        FoxTask(Matrix a, Matrix b, Matrix result,
                int rowStart, int rowEnd, int colStart, int colEnd) {
            this.a = a; this.b = b; this.result = result;
            this.rowStart = rowStart; this.rowEnd = rowEnd;
            this.colStart = colStart; this.colEnd = colEnd;
        }

        @Override
        protected void compute() {
            int rows = rowEnd - rowStart;
            int cols = colEnd - colStart;

            if (rows <= BLOCK_THRESHOLD && cols <= BLOCK_THRESHOLD) {
                int kDim = a.getCols();
                for (int k = 0; k < kDim; k += BLOCK_THRESHOLD) {
                    int kEnd = Math.min(k + BLOCK_THRESHOLD, kDim);

                    for (int i = rowStart; i < rowEnd; i++) {
                        for (int j = colStart; j < colEnd; j++) {
                            int sum = 0;
                            for (int p = k; p < kEnd; p++) {
                                sum += a.getValue(i, p) * b.getValue(p, j);
                            }
                            result.addValue(i, j, sum);
                        }
                    }
                }
            } else {
                // рекурсивне розбиття
                if (rows >= cols) {
                    int midRow = (rowStart + rowEnd) / 2;
                    FoxTask top    = new FoxTask(a, b, result, rowStart, midRow, colStart, colEnd);
                    FoxTask bottom = new FoxTask(a, b, result, midRow, rowEnd, colStart, colEnd);
                    top.fork();
                    bottom.compute();
                    top.join();
                } else {
                    int midCol = (colStart + colEnd) / 2;
                    FoxTask left  = new FoxTask(a, b, result, rowStart, rowEnd, colStart, midCol);
                    FoxTask right = new FoxTask(a, b, result, rowStart, rowEnd, midCol, colEnd);
                    left.fork();
                    right.compute();
                    left.join();
                }
            }
        }
    }

    public Matrix multiplyMatrixForkJoin() {
        Matrix fjResult = new Matrix(a.getRows(), b.getCols());

        ForkJoinPool pool = new ForkJoinPool(threadsNum);
        pool.invoke(new FoxTask(a, b, fjResult, 0, a.getRows(), 0, b.getCols()));
        pool.shutdown();

        return fjResult;
    }


    private static class FoxProcessThread extends Thread {

        private final Matrix a, b, result;
        private final int startRow, startCol, blockSize;

        FoxProcessThread(Matrix a, Matrix b, int startRow, int startCol, int blockSize, Matrix result) {
            this.a = a; this.b = b;
            this.startRow = startRow; this.startCol = startCol;
            this.blockSize = blockSize; this.result = result;
        }

        private int checkSize(int start, int sizeLimit) {
            return Math.min(blockSize, sizeLimit - start);
        }

        @Override
        public void run() {
            int aRowSize = checkSize(startRow, a.getRows());
            int bColSize = checkSize(startCol, b.getCols());

            for (int k = 0; k < a.getCols(); k += blockSize) {
                int aColSize = checkSize(k, a.getCols());
                int bRowSize = checkSize(k, b.getRows());

                Matrix blockA = sliceMatrix(a, startRow, startRow + aRowSize, k, k + aColSize);
                Matrix blockB = sliceMatrix(b, k, k + bRowSize, startCol, startCol + bColSize);
                Matrix resBlock = SequentialMethod.multiply(blockA, blockB);

                for (int i = 0; i < resBlock.getRows(); i++)
                    for (int j = 0; j < resBlock.getCols(); j++)
                        result.addValue(i + startRow, j + startCol, resBlock.getValue(i, j));
            }
        }

        private Matrix sliceMatrix(Matrix m, int rowStart, int rowEnd, int colStart, int colEnd) {
            Matrix slice = new Matrix(rowEnd - rowStart, colEnd - colStart);
            for (int i = rowStart; i < rowEnd; i++)
                for (int j = colStart; j < colEnd; j++)
                    slice.setValue(i - rowStart, j - colStart, m.getValue(i, j));
            return slice;
        }
    }
}
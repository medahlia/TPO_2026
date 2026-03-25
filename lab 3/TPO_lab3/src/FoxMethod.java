
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

    public Matrix multiplyMatrix() {
        int blockSize = (int) Math.ceil(1.0 * a.getRows() / (int) Math.sqrt(threadsNum));

        FoxProcessThread[] threads = new FoxProcessThread[threadsNum];
        int threadCounter = 0;

        for (int i = 0; i < a.getRows(); i += blockSize) {
            for (int j = 0; j < b.getCols(); j += blockSize) {
                threads[threadCounter] = new FoxProcessThread(a, b, i, j, blockSize, result);
                threadCounter++;
            }
        }

        for (int i = 0; i < threadCounter; i++)
            threads[i].start();

        for (int i = 0; i < threadCounter; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }

        return result;
    }

    // ---------------------------------------------------------------
    // Внутрішній клас потоку
    // ---------------------------------------------------------------
    private static class FoxProcessThread extends Thread {

        private final Matrix a;
        private final Matrix b;
        private final int startRow;
        private final int startCol;
        private final int blockSize;
        private final Matrix result;

        FoxProcessThread(Matrix a, Matrix b,
                         int startRow, int startCol,
                         int blockSize, Matrix result) {
            this.a = a;
            this.b = b;
            this.startRow = startRow;
            this.startCol = startCol;
            this.blockSize = blockSize;
            this.result = result;
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

                // отримуємо підматриці
                Matrix blockA = sliceMatrix(a, startRow, startRow + aRowSize, k, k + aColSize);
                Matrix blockB = sliceMatrix(b, k, k + bRowSize, startCol, startCol + bColSize);

                Matrix resBlock = new SequentialMethod().multiply(blockA, blockB);

                for (int i = 0; i < resBlock.getRows(); i++)
                    for (int j = 0; j < resBlock.getCols(); j++)
                        result.addValue(i + startRow, j + startCol, resBlock.getValue(i, j));
            }
        }

        // Метод для отримання підматриці
        private Matrix sliceMatrix(Matrix m, int rowStart, int rowEnd, int colStart, int colEnd) {
            Matrix slice = new Matrix(rowEnd - rowStart, colEnd - colStart);
            for (int i = rowStart; i < rowEnd; i++)
                for (int j = colStart; j < colEnd; j++)
                    slice.setValue(i - rowStart, j - colStart, m.getValue(i, j));
            return slice;
        }
    }
}
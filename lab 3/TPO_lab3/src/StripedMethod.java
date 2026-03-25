import java.util.ArrayList;

public class StripedMethod {

    public Matrix multiplyMatrix(Matrix a, Matrix b, int threadsCount) {

        if (a.getCols() != b.getRows())
            throw new IllegalArgumentException("It is impossible to multiply matrices due to their size");

        int aRows = a.getRows();
        int bCols = b.getCols();
        int commonDim = a.getCols();
        Matrix result = new Matrix(aRows, bCols);

        int rowsPerThread = aRows / threadsCount;
        ArrayList<Thread> threads = new ArrayList<>();

        for (int i = 0; i < threadsCount; i++) {
            int startRow = i * rowsPerThread;
            int endRow = (i == threadsCount - 1) ? aRows : (i + 1) * rowsPerThread;

            threads.add(new Thread(() -> {
                for (int row = startRow; row < endRow; row++) {
                    for (int col = 0; col < bCols; col++) {
                        int sum = 0;
                        for (int k = 0; k < commonDim; k++) {
                            sum += a.getValue(row, k) * b.getValue(k, col);
                        }
                        result.setValue(row, col, sum);
                    }
                }
            }));
        }

        // Запускаємо всі потоки
        for (Thread thread : threads) thread.start();

        // Чекаємо завершення всіх потоків
        try {
            for (Thread thread : threads) thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return result;
    }
}
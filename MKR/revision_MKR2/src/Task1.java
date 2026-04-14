// Напишіть фрагмент коду, в якому ForkJoin пул десяти потоків
// виконує обчислення суми 100000 дійсних чисел модифікованим каскадним алгоритмом.

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.Random;

public class Task1 { // ParallelSum

    private static final int ARRAY_SIZE = 100_000;
    private static final int THREAD_COUNT = 10;
    private static final int THRESHOLD = ARRAY_SIZE / THREAD_COUNT; // 10_000

    private static final ForkJoinPool pool = new ForkJoinPool(THREAD_COUNT);

    // ─── Каскадна рекурсивна задача ───────────────────────────────────────
    static class CascadeSumTask extends RecursiveTask<Double> {

        private final double[] data;
        private final int from;
        private final int to;

        CascadeSumTask(double[] data, int from, int to) {
            this.data = data;
            this.from = from;
            this.to   = to;
        }

        @Override
        protected Double compute() {
            int length = to - from;

            // Базовий випадок: підмасив достатньо малий — рахуємо послідовно
            if (length <= THRESHOLD) {
                double sum = 0.0;
                for (int i = from; i < to; i++) {
                    sum += data[i];
                }
                return sum;
            }

            // Каскадний поділ: ліва половина форкається асинхронно,
            // права виконується в поточному потоці (економимо потік)
            int mid = from + length / 2;

            CascadeSumTask leftTask  = new CascadeSumTask(data, from, mid);
            CascadeSumTask rightTask = new CascadeSumTask(data, mid,  to);

            leftTask.fork();                      // async у пул
            double rightResult = rightTask.compute(); // sync у поточному потоці
            double leftResult  = leftTask.join(); // чекаємо ліву

            return leftResult + rightResult;
        }
    }

    // ─── Генерація масиву ─────────────────────────────────────────────────
    static double[] generateData(int size) {
        Random rng = new Random(42); // seed для відтворюваності
        double[] result = new double[size];
        for (int i = 0; i < size; i++) {
            result[i] = rng.nextDouble() * 200.0 - 100.0; // [-100, 100]
        }
        return result;
    }

    // ─── Точка входу ──────────────────────────────────────────────────────
    public static void main(String[] args) {
        double[] data = generateData(ARRAY_SIZE);

        // Паралельне обчислення (ForkJoin, 10 потоків, каскадний алгоритм)
        CascadeSumTask rootTask = new CascadeSumTask(data, 0, data.length);
        double parallelResult = pool.invoke(rootTask);

        // Послідовне обчислення (для перевірки)
        double sequentialResult = 0.0;
        for (double v : data) {
            sequentialResult += v;
        }

        System.out.printf("Розмір масиву    : %,d%n",     ARRAY_SIZE);
        System.out.printf("Потоків у пулі   : %d%n",      THREAD_COUNT);
        System.out.printf("Поріг (threshold): %,d%n",     THRESHOLD);
        System.out.printf("Паралельна сума  : %.4f%n",    parallelResult);
        System.out.printf("Послідовна сума  : %.4f%n",    sequentialResult);
        System.out.printf("Різниця |Δ|      : %.2e%n",    Math.abs(parallelResult - sequentialResult));

        pool.shutdown();
    }
}

///////////////////////////////////////////////////////////////

private static final int ARRAY_SIZE = 100_000;
private static final int THREAD_COUNT = 10;
private static final int THRESHOLD = ARRAY_SIZE / THREAD_COUNT; // 10_000

private static final ForkJoinPool pool = new ForkJoinPool(THREAD_COUNT);


static class CascadeSumTask extends RecursiveTask<Double> {

    private final double[] data;
    private final int from;
    private final int to;

    CascadeSumTask(double[] data, int from, int to) {
        this.data = data;
        this.from = from;
        this.to = to;
    }

    @Override
    protected Double compute() {

        int length = to - from;

        if (length <= THRESHOLD) {
            double sum = 0.0;
            for (int i = from; i < to; i++) {
                sum += data[i];
            }
            return sum;
        }

        int mid = from + length / 2;

        CascadeSumTask leftTask = new CascadeSumTask(data, from, mid);
        CascadeSumTask rightTask = new CascadeSumTask(data, mid, to);

        leftTask.fork();
        double rightResult = rightTask.compute();
        double leftResult = leftTask.join();

        return leftResult + rightResult;
    }
}
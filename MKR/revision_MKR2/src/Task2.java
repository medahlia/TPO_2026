//Напишіть реалізацію паралельного середнього довжини
// слів за даними, що зберігаються у списку ArrayList<String> list
// з використанням ForkJoin фреймворку.

import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.Random;

public class Task2 { //AverageWordLength

    private static final int WORD_COUNT = 100_000;
    private static final int THREAD_COUNT = 10;
    private static final int THRESHOLD = WORD_COUNT / THREAD_COUNT;

    private static final ForkJoinPool pool = new ForkJoinPool();

    // ─── Рекурсивна задача: повертає суму довжин слів у підсписку ─────────
    static class WordLengthSumTask extends RecursiveTask<Long> {

        private final ArrayList<String> words;
        private final int from;
        private final int to;

        WordLengthSumTask(ArrayList<String> words, int from, int to) {
            this.words = words;
            this.from  = from;
            this.to    = to;
        }

        @Override
        protected Long compute() {
            int length = to - from;

            // Базовий випадок — рахуємо суму довжин звичайним циклом
            if (length <= THRESHOLD) {
                long sum = 0;
                for (int i = from; i < to; i++) {
                    sum += words.get(i).length();
                }
                return sum;
            }

            // Поділ навпіл: ліва задача — асинхронно, права — у поточному потоці
            int mid = from + length / 2;

            WordLengthSumTask leftTask  = new WordLengthSumTask(words, from, mid);
            WordLengthSumTask rightTask = new WordLengthSumTask(words, mid,  to);

            leftTask.fork();
            long rightResult = rightTask.compute();
            long leftResult  = leftTask.join();

            return leftResult + rightResult;
        }
    }

    // ─── Паралельне обчислення середньої довжини ──────────────────────────
    static double computeParallel(ArrayList<String> words) {
        if (words.isEmpty()) {
            return 0.0;
        }
        WordLengthSumTask rootTask = new WordLengthSumTask(words, 0, words.size());
        long totalLength = pool.invoke(rootTask);
        return (double) totalLength / words.size();
    }

    // ─── Послідовне обчислення (для перевірки) ────────────────────────────
    static double computeSequential(ArrayList<String> words) {
        if (words.isEmpty()) {
            return 0.0;
        }
        long totalLength = 0;
        for (int i = 0; i < words.size(); i++) {
            totalLength += words.get(i).length();
        }
        return (double) totalLength / words.size();
    }

    // ─── Генерація списку випадкових слів ─────────────────────────────────
    static ArrayList<String> generateWords(int count, int meanLen, int deviation) {
        Random rng = new Random(42);
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        ArrayList<String> result = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            // Нормальний розподіл довжин; мінімум 1 (не допускаємо порожніх слів)
            int wordLen = (int) Math.round(rng.nextGaussian() * deviation + meanLen);
            if (wordLen < 1) wordLen = 1;

            StringBuilder sb = new StringBuilder(wordLen);
            for (int j = 0; j < wordLen; j++) {
                sb.append(alphabet.charAt(rng.nextInt(alphabet.length())));
            }
            result.add(sb.toString());
        }
        return result;
    }

    // ─── Точка входу ──────────────────────────────────────────────────────
    public static void main(String[] args) {
        int meanWordLen = 8;
        int deviation   = 3;

        ArrayList<String> words = generateWords(WORD_COUNT, meanWordLen, deviation);

        double parallelAvg   = computeParallel(words);
        double sequentialAvg = computeSequential(words);

        System.out.printf("Кількість слів        : %,d%n",  WORD_COUNT);
        System.out.printf("Потоків у пулі        : %d%n",   THREAD_COUNT);
        System.out.printf("Поріг (threshold)     : %,d%n",  THRESHOLD);
        System.out.printf("Середня довжина (пар.): %.4f%n", parallelAvg);
        System.out.printf("Середня довжина (пос.): %.4f%n", sequentialAvg);
        System.out.printf("Різниця |Δ|           : %.2e%n", Math.abs(parallelAvg - sequentialAvg));

        pool.shutdown();
    }
}



///////////////////////////////////////////////////////////
import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.Random;

public class AverageWordLength {

    private static final int WORD_COUNT = 100_000;
    private static final int THREAD_COUNT = 10;
    private static final int THRESHOLD = WORD_COUNT / THREAD_COUNT;

    private static final ForkJoinPool pool = new ForkJoinPool();


    static class WordLengthSumTask extends RecursiveTask<Long> {

        private final ArrayList<String> words;
        private final int from;
        private final int to;

        WordLengthSumTask(ArrayList<String> words, int from, int to) {
            this.words = words;
            this.from = from;
            this.to = to;
        }

        @Override
        protected Long compute() {
            int length = to - from;

            if (length <= THRESHOLD) {
                long sum = 0;
                for (int i = from; i < to; i++) {
                    sum += words.get(i).length();
                }
                return sum;
            }

            int mid = from + length / 2;

            WordLengthSumTask leftTask  = new WordLengthSumTask(words, from, mid);
            WordLengthSumTask rightTask = new WordLengthSumTask(words, mid,  to);

            leftTask.fork();
            long rightResult = rightTask.compute();
            long leftResult  = leftTask.join();

            return leftResult + rightResult;
        }
    }

    static double computeParallel(ArrayList<String> words) {
        if (words.isEmpty()) {
            return 0.0;
        }
        WordLengthSumTask rootTask = new WordLengthSumTask(words, 0, words.size());
        long totalLength = pool.invoke(rootTask);
        return (double) totalLength / words.size();
    }

    static double computeSequential(ArrayList<String> words) {
        if (words.isEmpty()) {
            return 0.0;
        }
        long totalLength = 0;
        for (int i = 0; i < words.size(); i++) {
            totalLength += words.get(i).length();
        }
        return (double) totalLength / words.size();
    }

    static ArrayList<String> generateWords(int count, int meanLen, int deviation) {
        Random rng = new Random(42);
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        ArrayList<String> result = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            int wordLen = (int) Math.round(rng.nextGaussian() * deviation + meanLen);
            if (wordLen < 1) wordLen = 1;

            StringBuilder sb = new StringBuilder(wordLen);
            for (int j = 0; j < wordLen; j++) {
                sb.append(alphabet.charAt(rng.nextInt(alphabet.length())));
            }
            result.add(sb.toString());
        }
        return result;
    }


    public static void main(String[] args) {
        int meanWordLen = 8;
        int deviation   = 3;

        ArrayList<String> words = generateWords(WORD_COUNT, meanWordLen, deviation);

        double parallelAvg   = computeParallel(words);
        double sequentialAvg = computeSequential(words);

        System.out.printf("Кількість слів: %,d%n",  WORD_COUNT);
        System.out.printf("Середня довжина (пар.): %.4f%n", parallelAvg);
        System.out.printf("Середня довжина (пос.): %.4f%n", sequentialAvg);
        System.out.printf("Різниця: %.2e%n", Math.abs(parallelAvg - sequentialAvg));

        pool.shutdown();
    }
}

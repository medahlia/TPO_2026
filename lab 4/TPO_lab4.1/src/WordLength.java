import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WordLength {

    private static volatile int threshold = 2;
    private static volatile int threadCount = Runtime.getRuntime().availableProcessors();
    private static volatile int charThreshold = 65536;

    public static void setThreshold(int t) { threshold = t; }
    public static void setThreadCount(int count) { threadCount = count; }
    public static void setCharThreshold(int t) { charThreshold = t; }


    public static Stats analyzeParallel(List<String> texts) {
        TextTask.resetSplitCount();
        ForkJoinPool pool = new ForkJoinPool(threadCount);
        try {
            return pool.invoke(new StringTask(texts, 0, texts.size()));
        } finally {
            pool.shutdown();
        }
    }

    public static Stats analyzeParallelPaths(List<Path> paths) {
        TextTask.resetSplitCount();
        ForkJoinPool pool = new ForkJoinPool(threadCount);
        try {
            return pool.invoke(new TextTask(paths, 0, paths.size()));
        } finally {
            pool.shutdown();
        }
    }

    public static List<Path> readAllPaths(String folder) throws IOException {
        List<Path> paths = new ArrayList<>();
        Files.walk(Paths.get(folder)).forEach(path -> {
            if (Files.isRegularFile(path)) paths.add(path);
        });
        return paths;
    }

    public static List<String> readAllFiles(String folder) throws IOException {
        List<String> texts = new ArrayList<>();
        Files.walk(Paths.get(folder)).forEach(path -> {
            if (Files.isRegularFile(path)) {
                try { texts.add(Files.readString(path)); }
                catch (IOException e) { e.printStackTrace(); }
            }
        });
        return texts;
    }

    public static Stats analyzeSequential(List<String> texts) {
        Stats stats = new Stats();
        for (String text : texts) {
            // Той самий алгоритм що і в parallel — чесне порівняння
            analyzeTextRange(text, 0, text.length(), stats);
        }
        return stats;
    }

    public static void analyzeText(String text, Stats stats) {
        String[] words = text.split("\\W+");
        for (String word : words) {
            if (!word.isEmpty()) stats.add(word.length());
        }
    }

    public static void analyzeTextRange(String text, int from, int to, Stats stats) {
        int wordStart = -1;
        for (int i = from; i < to; i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                if (wordStart == -1) wordStart = i;
            } else {
                if (wordStart != -1) {
                    stats.add(i - wordStart);
                    wordStart = -1;
                }
            }
        }
        if (wordStart != -1 && to == text.length()) {
            stats.add(to - wordStart);
        }
    }

    public static int getSplitCount() { return TextTask.getSplitCount(); }

    static class Stats {
        private int count = 0;
        private int sum = 0;
        private int min = Integer.MAX_VALUE;
        private int max = Integer.MIN_VALUE;

        void add(int length) {
            count++; sum += length;
            if (length < min) min = length;
            if (length > max) max = length;
        }

        void merge(Stats other) {
            if (other.count == 0) return;
            count += other.count; sum += other.sum;
            min = Math.min(min, other.min);
            max = Math.max(max, other.max);
        }

        void print() {
            System.out.println("Words: " + count);
            System.out.println("Average length: " + (double) sum / count);
            System.out.println("Minimum length: " + min);
            System.out.println("Maximum length: " + max);
        }
    }

    static class StringTask extends RecursiveTask<Stats> {
        private final List<String> texts;
        private final int start, end;

        StringTask(List<String> texts, int start, int end) {
            this.texts = texts; this.start = start; this.end = end;
        }

        @Override
        protected Stats compute() {
            if (end - start <= threshold) {
                Stats stats = new Stats();
                for (int i = start; i < end; i++) {
                    String text = texts.get(i);
                    if (text.length() > charThreshold) {
                        stats.merge(new CharTask(text, 0, text.length()).compute());
                    } else {
                        analyzeTextRange(text, 0, text.length(), stats);
                    }
                }
                return stats;
            }
            int middle = (start + end) / 2;
            TextTask.splitCounter.incrementAndGet();
            StringTask left = new StringTask(texts, start, middle);
            StringTask right = new StringTask(texts, middle, end);
            left.fork();
            Stats rightResult = right.compute();
            Stats leftResult = left.join();
            leftResult.merge(rightResult);
            return leftResult;
        }
    }

    static class TextTask extends RecursiveTask<Stats> {
        static final AtomicInteger splitCounter = new AtomicInteger(0);
        private final List<Path> paths;
        private final int start, end;

        TextTask(List<Path> paths, int start, int end) {
            this.paths = paths; this.start = start; this.end = end;
        }

        @Override
        protected Stats compute() {
            if (end - start <= threshold) {
                Stats stats = new Stats();
                for (int i = start; i < end; i++) {
                    try {
                        String text = Files.readString(paths.get(i));
                        if (text.length() > charThreshold) {
                            stats.merge(new CharTask(text, 0, text.length()).compute());
                        } else {
                            analyzeTextRange(text, 0, text.length(), stats);
                        }
                    } catch (IOException e) { e.printStackTrace(); }
                }
                return stats;
            }
            int middle = (start + end) / 2;
            splitCounter.incrementAndGet();
            TextTask left = new TextTask(paths, start, middle);
            TextTask right = new TextTask(paths, middle, end);
            left.fork();
            Stats rightResult = right.compute();
            Stats leftResult = left.join();
            leftResult.merge(rightResult);
            return leftResult;
        }

        public static int getSplitCount() { return splitCounter.get(); }
        static void resetSplitCount() { splitCounter.set(0); }
    }

    static class CharTask extends RecursiveTask<Stats> {
        private final String text;
        private final int from, to;

        CharTask(String text, int from, int to) {
            this.text = text; this.from = from; this.to = to;
        }

        @Override
        protected Stats compute() {
            if (to - from <= charThreshold) {
                Stats stats = new Stats();
                analyzeTextRange(text, from, to, stats);
                return stats;
            }
            int middle = (from + to) / 2;
            while (middle < to && Character.isLetterOrDigit(text.charAt(middle))) middle++;
            CharTask left = new CharTask(text, from, middle);
            CharTask right = new CharTask(text, middle, to);
            left.fork();
            Stats rightResult = right.compute();
            Stats leftResult = left.join();
            leftResult.merge(rightResult);
            return leftResult;
        }
    }
}
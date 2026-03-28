import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class WordLength {

    private static ForkJoinPool pool =  new ForkJoinPool(); // автоматично створює кількість потоків = кількість ядер CPU

    public static Stats analyzeParallel(List<String> texts) {
        return pool.invoke(new TextTask(texts, 0, texts.size()));
    }

    public static List<String> readAllFiles(String folder) throws IOException {
        List<String> texts = new ArrayList<>();

        Path start = Paths.get(folder);

        Files.walk(start).forEach(path -> {
            if (Files.isRegularFile(path)) {
                try {
                    String content = Files.readString(path);
                    texts.add(content);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        return texts;
    }

    public static Stats analyzeSequential(List<String> texts) {
        Stats stats = new Stats();

        for (String text : texts) {
            analyzeText(text, stats);
        }

        return stats;
    }

    public static void analyzeText(String text, Stats stats) {
        String[] words = text.split("\\W+");

        for (String word : words) {
            if (!word.isEmpty()) {
                stats.add(word.length());
            }
        }
    }

    static class Stats {
        private int count = 0;
        private int sum = 0;

        private int max = Integer.MAX_VALUE;
        private int min = Integer.MIN_VALUE;

        void add(int length) {
            count++;
            sum += length;
            min = Math.min(min, length);
            max = Math.max(max, length);
        }

        void merge(Stats other) {
            count += other.count;
            sum += other.sum;
            min = Math.min(min, other.min);
            max = Math.max(max, other.max);
        }

        void print() {
            double avg = (double) sum / count;

            System.out.println("Words: " + count);
            System.out.println("Average length: " + avg);
            System.out.println("Minimum length: " + min);
            System.out.println("Maximum length: " + max);
        }
    }

    static class TextTask extends RecursiveTask<Stats> {
        private static final int THRESHOLD = 2;

        private final List<String> texts;
        private final int start;
        private final int end;

        TextTask(List<String> texts, int start, int end) {
            this.texts = texts;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Stats compute() {
            if (end - start <= THRESHOLD) {
                Stats stats = new Stats();
                for (int i = start; i < end; i++) {
                    analyzeText(texts.get(i), stats);
                }
                return stats;
            }

            int middle = (start + end) / 2;

            TextTask left = new TextTask(texts, start, middle);
            TextTask right = new TextTask(texts, middle, end);

            left.fork();
            Stats rightResult = right.compute();
            Stats leftResult = left.join();

            leftResult.merge(rightResult);
            return leftResult;
        }

    }


//    public int getThreadPoolSize() {
//        return pool.getParallelism(); // повертає кількість потоків
//    }
//
//
//    static class TextFile {
//        private final Path filePath;
//        private final String rawText; // текст напряму
//
//        public TextFile(Path path) {
//            this.filePath = path;
//            this.rawText = null;
//        }
//
//        public TextFile(String contents) { // Використовується коли файл вже розбитий на частини
//            this.filePath = null;
//            this.rawText = contents;
//        }
//
//        public String fetchText() throws IOException {
//            return filePath == null ? rawText : Files.readString(filePath);
//        }
//
//    }
}

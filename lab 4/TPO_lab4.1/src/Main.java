import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {

        int runs = 10;
        int[] thresholds = {2};
        int[] threadCounts = {2, 4, Runtime.getRuntime().availableProcessors()};

        for (int folderIndex = 1; folderIndex <= 7; folderIndex++) {


            String folder = "texts/texts" + folderIndex;
            // зберігання шляху
            var paths = WordLength.readAllPaths(folder);

            System.out.println("=============================");
            System.out.println("Folder: " + folder);
            System.out.println("Files count: " + paths.size());

            int defaultThreads = Runtime.getRuntime().availableProcessors();

            //System.out.println("\n--- THRESHOLD experiment ---");
            //System.out.printf("%-12s %-15s %-15s%n", "Threshold", "Avg Speedup", "Avg Efficiency");

            WordLength.setThreadCount(defaultThreads);

            for (int threshold : thresholds) {
                WordLength.setThreshold(threshold);

                for (int w = 0; w < 3; w++) {
                    WordLength.analyzeParallelPaths(paths);
                }

                double totalSpeedup = 0;

                for (int i = 0; i < runs; i++) {
                    System.gc(); Thread.sleep(50);
                    long startSeq = System.nanoTime();

                    for (Path p : paths) {
                        String text = Files.readString(p);
                        WordLength.Stats s = new WordLength.Stats();
                        WordLength.analyzeTextRange(text, 0, text.length(), s);
                    }
                    long endSeq = System.nanoTime();

                    System.gc(); Thread.sleep(50);
                    long startPar = System.nanoTime();
                    WordLength.analyzeParallelPaths(paths);
                    long endPar = System.nanoTime();

                    totalSpeedup += (endSeq - startSeq) / (double)(endPar - startPar);
                }

                double avgSpeedup = totalSpeedup / runs;
                //System.out.printf("%-12d %-15.3f %-15.3f%n",
                 //       threshold, avgSpeedup, avgSpeedup / defaultThreads);
            }

            // threads experiment
            System.out.println("\n--- THREADS experiment ---");
            System.out.printf("%-12s %-15s %-15s%n", "Threads", "Avg Speedup", "Avg Efficiency");

            WordLength.setThreshold(2);

            for (int threadCount : threadCounts) {
                WordLength.setThreadCount(threadCount);

                for (int w = 0; w < 3; w++) {
                    WordLength.analyzeParallelPaths(paths);
                }

                double totalSpeedup = 0;

                for (int i = 0; i < runs; i++) {
                    System.gc(); Thread.sleep(50);
                    long startSeq = System.nanoTime();
                    for (Path p : paths) {
                        String text = Files.readString(p);
                        WordLength.Stats s = new WordLength.Stats();
                        WordLength.analyzeTextRange(text, 0, text.length(), s);
                    }
                    long endSeq = System.nanoTime();

                    System.gc(); Thread.sleep(50);
                    long startPar = System.nanoTime();
                    WordLength.analyzeParallelPaths(paths);
                    long endPar = System.nanoTime();

                    totalSpeedup += (endSeq - startSeq) / (double)(endPar - startPar);
                }

                double avgSpeedup = totalSpeedup / runs;
                System.out.printf("%-12d %-15.3f %-15.3f%n",
                        threadCount, avgSpeedup, avgSpeedup / threadCount);
            }
        }
    }
}
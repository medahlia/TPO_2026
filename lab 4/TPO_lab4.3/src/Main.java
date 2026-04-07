import java.io.File;
import java.util.Set;

public class Main {

    public static void main(String[] args) {
        // Path to the folder with text files
        for (int indexFolder = 1; indexFolder <= 7; indexFolder++) {

            String targetPath = "/Users/home/TPO_2026/lab 4/TPO_lab4.1/texts/texts" + indexFolder;
            CommonWords.Folder root = CommonWords.Folder.load(new File(targetPath));

            CommonWords analyzer = new CommonWords();

            String folder = "texts" + indexFolder;
            System.out.println("=============================");
            System.out.println("Folder: " + folder);


            // --- JVM warm-up (result is discarded) ---
            analyzer.findCommonWordsSequential(root);
            analyzer.findCommonWordsParallel(root);

            // --- Single-threaded execution ---
            long t1Start = System.nanoTime();
            Set<String> seqResult = analyzer.findCommonWordsSequential(root);
            long t1End = System.nanoTime();
            double seqMs = (t1End - t1Start) / 1_000_000.0;

            System.out.println("=== Single-threaded mode ===");
            System.out.printf("Execution time: %.2f ms%n", seqMs);
            System.out.println("Common words: " + seqResult);

            System.out.println();

            // --- Parallel execution (ForkJoin) ---
            int threadCount = CommonWords.getThreadCount();
            long t2Start = System.nanoTime();
            Set<String> parResult = analyzer.findCommonWordsParallel(root);
            long t2End = System.nanoTime();
            double parMs = (t2End - t2Start) / 1_000_000.0;

            System.out.println("=== Parallel mode (ForkJoin) ===");
            System.out.printf("Thread count: %d%n", threadCount);
            System.out.printf("Execution time: %.2f ms%n", parMs);
            System.out.println("Common words: " + parResult);

            System.out.println();

            // --- Metrics ---
            double speedup = seqMs / parMs;
            double efficiency = speedup / threadCount;
            System.out.printf("Speedup: %.2f x%n", speedup);
            System.out.printf("Efficiency: %.4f%n", efficiency);
        }
    }
}
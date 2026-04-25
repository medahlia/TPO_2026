import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        int runs = 5;

        for (int folderIndex = 1; folderIndex <= 7; folderIndex++) {
            String path = "/Users/home/TPO_2026/lab 4/TPO_lab4.1/texts/texts" + folderIndex;
            KeywordSearch.Folder root = KeywordSearch.Folder.load(new File(path));

            KeywordSearch searcher = new KeywordSearch();
            String folderName = "texts" + folderIndex;

            System.out.println("=============================");
            System.out.println("Folder: " + folderName);

            System.out.print("Enter keywords: ");
            Scanner scanner = new Scanner(System.in);
            List<String> keywords = parseKeywords(scanner.nextLine());
            System.out.println("Keywords: " + keywords);

            searcher.findSequential(root, keywords);
            searcher.findParallel(root, keywords);

            double totalSeq = 0;
            double totalPar = 0;

            List<String> seqResult = null;
            List<String> parResult = null;

            for (int i = 0; i < runs; i++) {
                long t1 = System.nanoTime();
                seqResult = searcher.findSequential(root, keywords);
                totalSeq += System.nanoTime() - t1;

                long t2 = System.nanoTime();
                parResult = searcher.findParallel(root, keywords);
                totalPar += System.nanoTime() - t2;
            }

            double avgSeqMs = totalSeq / runs / 1_000_000.0;
            double avgParMs = totalPar / runs / 1_000_000.0;
            int    threads  = KeywordSearch.getThreadCount();
            double speedup  = avgSeqMs / avgParMs;

            System.out.println("=== Results (average over " + runs + " runs) ===");
            System.out.printf("Sequential time : %.2f ms%n", avgSeqMs);
            System.out.printf("Parallel time   : %.2f ms%n", avgParMs);
            System.out.printf("Threads         : %d%n",      threads);
            System.out.printf("Speedup         : %.2f x%n",  speedup);
            System.out.printf("Efficiency      : %.4f%n",    speedup / threads);
            System.out.println("Matches (seq)   : " + seqResult.size());
            for (String r : seqResult) System.out.println("  " + r);
        }
    }

    static List<String> parseKeywords(String input) {
        return Arrays.stream(input.split("\\W+"))
                .map(String::toLowerCase)
                .filter(w -> !w.isEmpty())
                .toList();
    }
}
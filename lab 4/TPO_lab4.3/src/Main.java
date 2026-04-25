import java.io.File;
import java.util.Set;

public class Main {

    public static void main(String[] args) {

        int runs = 5;

        for (int indexFolder = 1; indexFolder <= 7; indexFolder++) {

            String targetPath = "/Users/home/TPO_2026/lab 4/TPO_lab4.1/texts/texts" + indexFolder;
            CommonWords.Folder root = CommonWords.Folder.load(new File(targetPath));

            CommonWords analyzer = new CommonWords();

            String folder = "texts" + indexFolder;
            System.out.println("=============================");
            System.out.println("Folder: " + folder);


            analyzer.findCommonWordsSequential(root);
            analyzer.findCommonWordsParallel(root);

            double totalSeq = 0;
            double totalPar = 0;

            Set<String> seqResult = null;
            Set<String> parResult = null;

            for (int i = 0; i < runs; i++) {

                long t1Start = System.nanoTime();
                seqResult = analyzer.findCommonWordsSequential(root);
                long t1End = System.nanoTime();
                totalSeq += (t1End - t1Start);

                long t2Start = System.nanoTime();
                parResult = analyzer.findCommonWordsParallel(root);
                long t2End = System.nanoTime();
                totalPar += (t2End - t2Start);
            }

            double avgSeqMs = totalSeq / runs / 1_000_000.0;
            double avgParMs = totalPar / runs / 1_000_000.0;

            int threadCount = CommonWords.getThreadCount();

            double speedup = avgSeqMs / avgParMs;
            double efficiency = speedup / threadCount;


            System.out.println("=== Results (average over " + runs + " runs) ===");

            System.out.printf("Sequential time: %.2f ms%n", avgSeqMs);
            System.out.printf("Parallel time:   %.2f ms%n", avgParMs);

            System.out.println("Common words: " + seqResult);

            System.out.println();
            System.out.printf("Speedup: %.2f x%n", speedup);
            System.out.printf("Efficiency: %.4f%n", efficiency);
        }
    }
}
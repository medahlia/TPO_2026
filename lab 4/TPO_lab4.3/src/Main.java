import java.io.File;
import java.util.Set;

public class Main {

    public static void main(String[] args) {
        // Шлях до папки з текстовими файлами
        String targetPath = "D:\\tpo\\lab-4-docs\\texts";
        CommonWords.Folder root = CommonWords.Folder.load(new File(targetPath));

        CommonWords analyzer = new CommonWords();

        // --- Однопотокове виконання ---
        long t1Start = System.nanoTime();
        Set<String> seqResult = analyzer.findCommonWordsSequential(root);
        long t1End = System.nanoTime();
        double seqMs = (t1End - t1Start) / 1_000_000.0;

        System.out.println("=== Однопотоковий режим ===");
        System.out.printf("Час виконання: %.2f мс%n", seqMs);
        System.out.println("Спільні слова: " + seqResult);

        System.out.println();

        // --- Паралельне виконання (ForkJoin) ---
        int threadCount = CommonWords.getThreadCount();
        long t2Start = System.nanoTime();
        Set<String> parResult = analyzer.findCommonWordsParallel(root);
        long t2End = System.nanoTime();
        double parMs = (t2End - t2Start) / 1_000_000.0;

        System.out.println("=== Паралельний режим (ForkJoin) ===");
        System.out.printf("Кількість потоків: %d%n", threadCount);
        System.out.printf("Час виконання: %.2f мс%n", parMs);
        System.out.println("Спільні слова: " + parResult);

        System.out.println();

        // --- Метрики ---
        double speedup = seqMs / parMs;
        double efficiency = speedup / threadCount;
        System.out.printf("Прискорення (speedup): %.2f x%n", speedup);
        System.out.printf("Ефективність (efficiency): %.4f%n", efficiency);
    }
}
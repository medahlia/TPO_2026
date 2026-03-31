public class Main {

    public static void main(String[] args) throws Exception {

        int runs = 10;
        int threads = Runtime.getRuntime().availableProcessors();

        for (int folderIndex = 1; folderIndex <= 7; folderIndex++) {

            String folder = "texts" + folderIndex;

            double totalSpeedup = 0;
            double totalEfficiency = 0;

            var texts = WordLength.readAllFiles(folder);

            for (int i = 0; i < runs; i++) {

                long startSeq = System.nanoTime();
                var seq = WordLength.analyzeSequential(texts);
                long endSeq = System.nanoTime();

                long startPar = System.nanoTime();
                var par = WordLength.analyzeParallel(texts);
                long endPar = System.nanoTime();

                double seqTime = (endSeq - startSeq) / 1e6;
                double parTime = (endPar - startPar) / 1e6;

                double speedup = seqTime / parTime;
                double efficiency = speedup / threads;

                totalSpeedup += speedup;
                totalEfficiency += efficiency;
            }

            double avgSpeedup = totalSpeedup / runs;
            double avgEfficiency = totalEfficiency / runs;

            System.out.println("Folder: " + folder);
            System.out.println("Files count: " + texts.size());
            System.out.println("Average Speedup: " + avgSpeedup);
            System.out.println("Average Efficiency: " + avgEfficiency);
            System.out.println("-----------------------------");
        }
    }
}
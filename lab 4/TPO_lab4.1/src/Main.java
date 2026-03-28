public class Main {

    public static void main(String[] args) throws Exception {

        String folder = "texts";

        var texts = WordLength.readAllFiles(folder);

        long startSeq = System.nanoTime();
        var seq = WordLength.analyzeSequential(texts);
        long endSeq = System.nanoTime();

        long startPar = System.nanoTime();
        var par = WordLength.analyzeParallel(texts);
        long endPar = System.nanoTime();

        double seqTime = (endSeq - startSeq) / 1e6;
        double parTime = (endPar - startPar) / 1e6;

        System.out.println("Sequential time: " + seqTime);
        seq.print();

        System.out.println();

        System.out.println("Parallel time: " + parTime);
        par.print();

        System.out.println("Speedup: " + seqTime / parTime);
    }
}
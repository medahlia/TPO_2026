public class PrintSomeSymbol {
    public static void main(String[] args) throws InterruptedException {
        int numThreads = 3;

        SyncInt permission = new SyncInt();

        Thread first = new Thread(new SomeSymbolSynchTest('|', permission, numThreads, 0));
        Thread second = new Thread(new SomeSymbolSynchTest('\\', permission, numThreads, 1));
        Thread third = new Thread(new SomeSymbolSynchTest('/', permission, numThreads, 2));

        first.start();
        second.start();
        third.start();

        first.join();
        second.join();
        third.join();
    }
}

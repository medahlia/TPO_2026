package withoutSync;

public class PrintSomeSymbol {
    public static void main(String[] args) throws InterruptedException {
        int count = 2700;

        Thread first  = new Thread(new SomeSymbolSynchTest('|',  count));
        Thread second = new Thread(new SomeSymbolSynchTest('\\', count));
        Thread third  = new Thread(new SomeSymbolSynchTest('/',  count));

        first.start();
        second.start();
        third.start();

        first.join();
        second.join();
        third.join();
    }
}

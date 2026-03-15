package withoutSync;

public class SomeSymbolSynchTest implements Runnable {
    private final char s;
    private final int count;

    public SomeSymbolSynchTest(char symbol, int count) {
        this.s = symbol;
        this.count = count;
    }

    @Override
    public void run() {
        for (int i = 1; i <= count; i++) {
            System.out.print(s);
            if (i % 90 == 0)
                System.out.println();
        }
    }
}

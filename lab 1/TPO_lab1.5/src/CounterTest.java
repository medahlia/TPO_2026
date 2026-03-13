public class CounterTest {
    private static final int ITERATIONS = 100000;

    public static void main(String[] args) throws InterruptedException {
        testUnsynchronized();
        testSynchronizedMethods();
        testSynchronizedBlocks();
        testObjectLock();
    }

    private static void testUnsynchronized() throws InterruptedException {
        Counter counter = new Counter();

        Thread incrementThread = new Thread(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                counter.increment();
            }
        });

        Thread decrementThread = new Thread(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                counter.decrement();
            }
        });

        incrementThread.start();
        decrementThread.start();

        incrementThread.join();
        decrementThread.join();

        System.out.println("Counter Result: " + counter.getCount());
    }

    private static void testSynchronizedMethods() throws InterruptedException {
        Counter counter = new Counter();

        Thread incrementThread = new Thread(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                counter.incrementSync();
            }
        });

        Thread decrementThread = new Thread(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                counter.decrementSync();
            }
        });

        incrementThread.start();
        decrementThread.start();

        incrementThread.join();
        decrementThread.join();

        System.out.println("Synchronized Methods Result: " + counter.getCount());
    }

    private static void testSynchronizedBlocks() throws InterruptedException {
        Counter counter = new Counter();

        Thread incrementThread = new Thread(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                counter.incrementWithBlock();
            }
        });

        Thread decrementThread = new Thread(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                counter.decrementWithBlock();
            }
        });

        incrementThread.start();
        decrementThread.start();

        incrementThread.join();
        decrementThread.join();

        System.out.println("Synchronized Blocks Result: " + counter.getCount());
    }

    private static void testObjectLock() throws InterruptedException {
        Counter counter = new Counter();

        Thread incrementThread = new Thread(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                counter.incrementWithLock();
            }
        });

        Thread decrementThread = new Thread(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                counter.decrementWithLock();
            }
        });

        incrementThread.start();
        decrementThread.start();

        incrementThread.join();
        decrementThread.join();

        System.out.println("Object Lock Result: " + counter.getCount());
    }
}
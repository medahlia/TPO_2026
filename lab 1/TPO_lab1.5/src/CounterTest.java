public class CounterTest {
    private static final int ITERATIONS = 100000;

    public static void main(String[] args) throws InterruptedException {
        testUnsynchronized();
        testSynchronizedMethods();
        testSynchronizedBlocks();
        testCounterAtomic();
    }

    private static void testUnsynchronized() throws InterruptedException {
        Counter counter = new Counter();

        long start = System.nanoTime(); // початок вимірювання

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

        long end = System.nanoTime(); // кінець вимірювання
        long durationMs = (end - start) / 1_000_000; // перетворюємо в мілісекунди

        System.out.println("Counter Result: " + counter.getCount() +
                " | Time: " + durationMs + " ms");
    }

    private static void testSynchronizedMethods() throws InterruptedException {
        Counter counter = new Counter();

        long start = System.nanoTime();

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

        long end = System.nanoTime();
        long durationMs = (end - start) / 1_000_000;

        System.out.println("Synchronized Methods Result: " + counter.getCount() +
                " | Time: " + durationMs + " ms");
    }

    private static void testSynchronizedBlocks() throws InterruptedException {
        Counter counter = new Counter();

        long start = System.nanoTime();

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

        long end = System.nanoTime();
        long durationMs = (end - start) / 1_000_000;

        System.out.println("Synchronized Blocks Result: " + counter.getCount() +
                " | Time: " + durationMs + " ms");

    }

    private static void testCounterAtomic() throws InterruptedException {
        CounterAtomic counter = new CounterAtomic();

        long start = System.nanoTime();

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

        long end = System.nanoTime();
        long durationMs = (end - start) / 1_000_000;

        System.out.println("Atomic Counter Result: " + counter.getCount() +
                " | Time: " + durationMs + " ms");
    }
}
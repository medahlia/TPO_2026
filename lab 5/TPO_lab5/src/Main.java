import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


class RunResult {
    final int runId;
    final int produced;
    final int serviced;
    final int refused;
    final double avgQueueLen;

    RunResult(int runId, int produced, int serviced, int refused, double avgQueueLen) {
        this.runId = runId;
        this.produced = produced;
        this.serviced = serviced;
        this.refused = refused;
        this.avgQueueLen = avgQueueLen;
    }

    void print() {
        System.out.printf("%n--- Симуляція #%d ---%n", runId);
        System.out.printf("Заявок згенеровано : %d%n",    produced);
        System.out.printf("Обслуговано        : %d%n",    serviced);
        System.out.printf("Відмов             : %d%n",    refused);
        System.out.printf("Ймовірність відмови: %.2f %%%n",
                produced > 0 ? 100.0 * refused / produced : 0.0);
        System.out.printf("Середня черга      : %.4f%n",  avgQueueLen);
    }
}

class QueueSimulation {

    // параметри моделі
    private static final int QUEUE_CAPACITY = 7;
    private static final int NUM_CHANNELS = 4;
    private static final int TARGET_REQUESTS = 1000;
    private static final long ARRIVE_MIN_MS = 5;
    private static final long ARRIVE_MAX_MS = 100;
    private static final double SERVICE_MEAN_MS = 200;
    private static final double SERVICE_STD_MS = 50;
    private static final long MONITOR_PERIOD_MS = 1000;

    private final int runId;
    private final BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final AtomicInteger produced = new AtomicInteger(0);
    private final AtomicInteger serviced = new AtomicInteger(0);
    private final AtomicInteger refused  = new AtomicInteger(0);
    private final List<Integer> queueSamples = new CopyOnWriteArrayList<>();
    private final Random rng = new Random();

    QueueSimulation(int runId) {
        this.runId = runId;
    }

    // генератор заявок
    private class Producer implements Runnable {
        @Override
        public void run() {
            while (produced.get() < TARGET_REQUESTS) {
                int id = produced.incrementAndGet();

                if (!queue.offer(id)) {
                    refused.incrementAndGet();
                }

                long delay = ARRIVE_MIN_MS +
                        (long)(rng.nextDouble() * (ARRIVE_MAX_MS - ARRIVE_MIN_MS));
                try { Thread.sleep(delay); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
    }

    // канал обслуговування NUM_CHANNELS
    private class Channel implements Runnable {
        @Override
        public void run() {
            while (produced.get() < TARGET_REQUESTS || !queue.isEmpty()) {
                try {
                    Integer req = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (req != null) {
                        double t = Math.max(1,
                                SERVICE_MEAN_MS + SERVICE_STD_MS * rng.nextGaussian());
                        Thread.sleep((long) t);
                        serviced.incrementAndGet();
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
    }

    //монітор стану
    private class Monitor implements Runnable {
        @Override
        public void run() {
            while (produced.get() < TARGET_REQUESTS || !queue.isEmpty()) {
                int sample = queue.size();
                queueSamples.add(sample);
                System.out.printf(
                        "[#%d] черга=%d  обслуговано=%d  відмов=%d%n",
                        runId, sample, serviced.get(), refused.get()
                );
                try { Thread.sleep(MONITOR_PERIOD_MS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
    }


    RunResult run() {
        ExecutorService channelPool = Executors.newFixedThreadPool(NUM_CHANNELS);
        for (int i = 0; i < NUM_CHANNELS; i++) {
            channelPool.submit(new Channel());
        }

        Thread producerThread = new Thread(new Producer());
        Thread monitorThread = new Thread(new Monitor());

        producerThread.start();
        monitorThread.start();

        try { producerThread.join(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        channelPool.shutdown();
        try { channelPool.awaitTermination(5, TimeUnit.MINUTES); }
        catch (InterruptedException e) {
            channelPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        try { monitorThread.join(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        double avgQueue = queueSamples.stream()
                .mapToInt(Integer::intValue).average().orElse(0.0);

        return new RunResult(runId, produced.get(), serviced.get(), refused.get(), avgQueue);
    }
}

public class Main {

    private static final int NUM_RUNS = 20;

    public static void main(String[] args) {
        ExecutorService simPool = Executors.newFixedThreadPool(NUM_RUNS);
        List<Future<RunResult>> futures = new ArrayList<>();

        for (int i = 1; i <= NUM_RUNS; i++) {
            final int id = i;
            futures.add(simPool.submit(() -> new QueueSimulation(id).run()));
        }

        simPool.shutdown();

        // збираємо результати
        List<RunResult> results = new ArrayList<>();
        for (Future<RunResult> f : futures) {
            try { results.add(f.get()); }
            catch (InterruptedException | ExecutionException e) { e.printStackTrace(); }
        }

        // кожна симуляцію
        for (RunResult r : results) {
            r.print();
        }

        double totalRefuseProb = 0;
        double totalAvgQueue = 0;

        for (RunResult r : results) {
            totalRefuseProb += (r.produced > 0 ? 100.0 * r.refused / r.produced : 0);
            totalAvgQueue += r.avgQueueLen;
        }

        System.out.println("\n==============================");
        System.out.printf("Результати %d симуляцій:%n", NUM_RUNS);
        System.out.printf("Середня ймовірність відмови : %.2f %%%n", totalRefuseProb / NUM_RUNS);
        System.out.printf("Середня довжина черги       : %.4f%n",    totalAvgQueue   / NUM_RUNS);
    }
}
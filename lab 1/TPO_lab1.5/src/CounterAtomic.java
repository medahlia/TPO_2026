import java.util.concurrent.atomic.AtomicInteger;


public class CounterAtomic {
    private AtomicInteger count = new AtomicInteger(0);

    public void increment() {
        count.incrementAndGet();
    }

    public void decrement() {
        count.decrementAndGet();
    }

    public int getCount() {
        return count.get();
    }

    public void resetCount() {
        count.set(0);
    }
}

import java.util.concurrent.atomic.AtomicInteger;

public class Counter {
    private int count = 0;

    // синхронізовані методи
    public synchronized void incrementSync() {
        count++;
    }

    public synchronized void decrementSync() {
        count--;
    }

    // синхронізовані блоки
    public void incrementWithBlock() {
        synchronized(this) {
            count++;
        }
    }

    public void decrementWithBlock() {
        synchronized(this) {
            count--;
        }
    }

    // без синхронізації
    public void increment() {
        count++;
    }

    public void decrement() {
        count--;
    }

    public int getCount() {
        return count;
    }

    public void resetCount() {
        count = 0;
    }
}


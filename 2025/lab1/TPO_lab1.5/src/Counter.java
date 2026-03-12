public class Counter {
    private int count = 0;
    private final Object lock = new Object();

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

    // синхронізація lock
    public void incrementWithLock() {
        synchronized(lock) {
            count++;
        }
    }

    public void decrementWithLock() {
        synchronized(lock) {
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


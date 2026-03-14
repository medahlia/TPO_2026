package atomicTypes;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

class Bank {
    public static final int NTEST = 10000;
    private final AtomicIntegerArray accounts;
    private final AtomicLong ntransacts = new AtomicLong(0);

    public Bank(int n, int initialBalance) {
        accounts = new AtomicIntegerArray(n);
        for (int i = 0; i < accounts.length(); i++)
            accounts.set(i, initialBalance);
        ntransacts.set(0);
    }

    public void transfer(int from, int to, int amount) {
        //synchronized (this) {
            accounts.addAndGet(from, -amount);
            accounts.addAndGet(to, amount);
            if (ntransacts.incrementAndGet() % NTEST == 0) {
                test();
                Thread.currentThread().interrupt();
            }
        //}
    }

    public void test() {
        AtomicInteger sum = new AtomicInteger(0);
        for (int i = 0; i < accounts.length(); i++)
            sum.addAndGet(accounts.get(i));
        System.out.println("Transactions:" + ntransacts.get()
                + " Sum: " + sum.get());
    }

    public int size() {
        return accounts.length();
    }
}

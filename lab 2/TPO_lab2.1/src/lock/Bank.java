package lock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Bank {
    public static final int NTEST = 10000;
    private final int[] accounts;
    private long ntransacts = 0;

    private final Lock bankLock = new ReentrantLock();
    private final Condition sufficientFunds = bankLock.newCondition();

    public Bank(int n, int initialBalance) {
        accounts = new int[n];
        for (int i = 0; i < accounts.length; i++)
            accounts[i] = initialBalance;
        ntransacts = 0;
    }

    // базовий варіант з Lock (без перевірки балансу)
    public void transfer(int from, int to, int amount) {
        bankLock.lock();
        try {
            accounts[from] -= amount;
            accounts[to] += amount;
            ntransacts++;
            if (ntransacts % NTEST == 0)
                test();
        } finally {
            bankLock.unlock();
        }
    }

    // варіант з очікуванням достатнього балансу (аналог waitTransfer)
    public void waitTransfer(int from, int to, int amount) {
        bankLock.lock();
        try {
            while (accounts[from] < amount) {
                sufficientFunds.await(); 
            }
            accounts[from] -= amount;
            accounts[to] += amount;
            ntransacts++;
            if (ntransacts % NTEST == 0)
                test();
            sufficientFunds.signalAll();
        } catch (InterruptedException ex) {
            Logger.getLogger(Bank.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            bankLock.unlock();
        }
    }

    public void test() {
        int sum = 0;
        for (int i = 0; i < accounts.length; i++) {
            sum += accounts[i];
        }
        System.out.println("Transactions:" + ntransacts + " Sum: " + sum);
    }

    public int size() {
        return accounts.length;
    }
}

public class SyncInt {
    private int num;
    private int permission;
    private boolean stop;

    public int getPermission() {
        return permission;
    }

    public synchronized boolean isStop() {
        return stop;
    }

    public synchronized void waitAndChange(int control, int maxControl, char s) {
        while (getPermission() % maxControl != control) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        System.out.print(s);
        permission++;
        num++;

        if (num % (maxControl * 30) == 0)
            System.out.println();

        if (num == maxControl * 30 * 90)
            stop = true;

        notifyAll();
    }
}

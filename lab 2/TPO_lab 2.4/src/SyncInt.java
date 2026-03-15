import java.util.logging.Level;
import java.util.logging.Logger;

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
                //System.out.println("\n" + s + " will wait");
                wait();
            } catch (InterruptedException e) {
                //Logger.getLogger(Sync.class.getName()).log(Level.SEVERE, null, e);
            }
        }
        System.out.println(s);
        permission++;
        num++;
        if (num % 99 == 0)
            System.out.println();

        if (num == 9900)
            stop = true;
        notifyAll();
    }
}

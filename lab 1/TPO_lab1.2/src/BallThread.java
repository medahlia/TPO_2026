public class BallThread extends Thread {
    private Ball b;

    public BallThread(Ball ball) {
        b = ball;
    }

    @Override
    public void run() {
        try {
            while (b.isAlive()) { // чи "жива" кулька, а не потік
                b.move();
                Thread.sleep(5);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    } // state: terminated
}

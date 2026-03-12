public class BallThread extends Thread {
    private Ball b;
    private BallThread threadToJoin;

    public BallThread(Ball ball) {
        this.b = ball;
        this.threadToJoin = null;
    }

    public BallThread(Ball ball, BallThread threadToJoin) {
        this.b = ball;
        this.threadToJoin = threadToJoin;
    }

    @Override
    public void run() {
        try {
            // якщо є потік для join, чекаємо на його завершення
            if (threadToJoin != null) {
                threadToJoin.join();
            }

            // кулька рухається, поки вона не потрапить в лузу
            while (!b.isFinished()) {
                b.move();
                System.out.println("Thread name = " + Thread.currentThread().getName());
                Thread.sleep(5);
            }
        } catch (InterruptedException ex) {
            //
        }
    }
}